package bf.anptic.geoportail.controller;

import bf.anptic.geoportail.dto.AnpticStatusDto;
import bf.anptic.geoportail.dto.IncidentDto;
import bf.anptic.geoportail.dto.LanStatusDto;
import bf.anptic.geoportail.dto.MapSiteDto;
import bf.anptic.geoportail.dto.RegisterTokenRequest;
import bf.anptic.geoportail.dto.SiteNetworkDto;
import bf.anptic.geoportail.dto.SiteSummaryDto;
import bf.anptic.geoportail.dto.TicketIncidentDto;
import bf.anptic.geoportail.model.Site;
import bf.anptic.geoportail.repository.EquipmentRepository;
import bf.anptic.geoportail.repository.SiteRepository;
import bf.anptic.geoportail.security.AccessScopeHolder;
import bf.anptic.geoportail.service.AnpticStatusService;
import bf.anptic.geoportail.service.IncidentService;
import bf.anptic.geoportail.service.ItopIncidentService;
import bf.anptic.geoportail.service.LanStatusService;
import bf.anptic.geoportail.service.MapService;
import bf.anptic.geoportail.service.NotificationService;
import bf.anptic.geoportail.service.SiteNetworkService;
import bf.anptic.geoportail.service.backoffice.AdminSupervisionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import bf.anptic.geoportail.dto.SiteStatutSimpleDto;
import bf.anptic.geoportail.model.enums.NodeStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class SiteController {

    private final SiteRepository siteRepository;
    private final EquipmentRepository equipmentRepository;
    private final AnpticStatusService anpticStatusService;
    private final LanStatusService lanStatusService;
    private final NotificationService notificationService;
    private final MapService mapService;
    private final SiteNetworkService siteNetworkService;
    private final IncidentService incidentService;
    private final ItopIncidentService itopIncidentService;
    private final AdminSupervisionService supervisionService;

    public SiteController(SiteRepository siteRepository,
                           EquipmentRepository equipmentRepository,
                           AnpticStatusService anpticStatusService,
                           LanStatusService lanStatusService,
                           NotificationService notificationService,
                           MapService mapService,
                           SiteNetworkService siteNetworkService,
                           IncidentService incidentService,
                           ItopIncidentService itopIncidentService,
                           AdminSupervisionService supervisionService) {
        this.siteRepository = siteRepository;
        this.equipmentRepository = equipmentRepository;
        this.anpticStatusService = anpticStatusService;
        this.lanStatusService = lanStatusService;
        this.notificationService = notificationService;
        this.mapService = mapService;
        this.siteNetworkService = siteNetworkService;
        this.incidentService = incidentService;
        this.itopIncidentService = itopIncidentService;
        this.supervisionService = supervisionService;
    }

    // Retour utilisateur (cote decideur) : un site sans aucun equipement
    // rattache n'a pas de statut ANPTIC/LAN exploitable et ne doit plus
    // apparaitre dans la liste des sites d'un ministere - memes 2
    // pipelines de synchronisation NetXMS independants (sites vs
    // equipements) qui ont motive le meme filtre cote Backoffice
    // (AdminSiteController - "avec equipement uniquement"). Requete
    // groupee (1 seule requete pour tous les sites) plutot qu'un comptage
    // par site, cf. EquipmentRepository.countGroupedBySiteId.
    private List<Site> avecEquipement(List<Site> sites) {
        Map<String, Integer> comptes = new HashMap<>();
        for (Object[] ligne : equipmentRepository.countGroupedBySiteId()) {
            comptes.put((String) ligne[0], ((Long) ligne[1]).intValue());
        }
        return sites.stream()
                .filter(site -> comptes.getOrDefault(site.getSiteId(), 0) > 0)
                .toList();
    }

    @GetMapping("/sites")
    public List<SiteSummaryDto> listSites() {
        String ministere = AccessScopeHolder.getMinistere();
        List<Site> sites = ministere == null
                ? siteRepository.findByActifTrue()
                : siteRepository.findByActifTrueAndMinistere(ministere);
        sites = avecEquipement(sites);

        Map<String, Integer> intervalles = supervisionService.getIntervallesActualisation();
        return sites.stream()
                .map(site -> new SiteSummaryDto(
                        site.getSiteId(),
                        site.getNom(),
                        site.getVille(),
                        intervalles.getOrDefault(site.getSiteId(), AdminSupervisionService.DEFAUT_INTERVALLE_S)
                ))
                .toList();
    }

    @GetMapping("/site/{id}/anptic")
    public AnpticStatusDto getAnpticStatus(@PathVariable("id") String siteId) {
        verifierAccesSite(siteId);
        return anpticStatusService.getAnpticStatus(siteId);
    }

    @GetMapping("/site/{id}/lan")
    public LanStatusDto getLanStatus(@PathVariable("id") String siteId) {
        verifierAccesSite(siteId);
        return lanStatusService.getLanStatus(siteId);
    }

    @GetMapping("/site/{id}/reseau")
    public SiteNetworkDto getSiteNetwork(@PathVariable("id") String siteId) {
        verifierAccesSite(siteId);
        return siteNetworkService.getSiteNetwork(siteId);
    }

    @PostMapping("/site/{id}/notifications/register")
    public void registerNotificationToken(@PathVariable("id") String siteId,
                                           @RequestBody RegisterTokenRequest request) {
        verifierAccesSite(siteId);
        notificationService.registerToken(siteId, request);
    }

    @GetMapping("/sites/map")
    public List<MapSiteDto> listSitesForMap() {
        List<MapSiteDto> tousLesSites = mapService.listSitesForMap();
        return filtrerParMinistere(tousLesSites, MapSiteDto::siteId);
    }

        // Vue simplifiee pour les utilisateurs lambda : statut global
    // uniquement (OK/KO), sans details techniques. Le statut WARN et
    // UNKNOWN sont ramenes a OK et KO respectivement pour rester
    // comprehensibles par un non-technicien.
    @GetMapping("/sites/statut-simple")
    public List<SiteStatutSimpleDto> listSitesStatutSimple() {
        return mapService.listSitesForMap().stream()
                .map(s -> new SiteStatutSimpleDto(
                        s.siteId(),
                        s.nom(),
                        s.ville(),
                        null,
                        null,
                        s.latitude(),
                        s.longitude(),
                        // WARN → KO (probleme), UNKNOWN → KO (pas de signal = pas bon)
                        // seul OK reste OK
                        // BUG CORRIGE : statutGlobal() renvoie l'enum NodeStatus, pas un
                        // String - "OK".equals(enum) est TOUJOURS false quel que soit le
                        // statut reel, ce qui forcait tous les sites en KO. Il faut
                        // comparer l'enum a l'enum, pas a une chaine.
                        s.statutGlobal() == NodeStatus.OK ? "OK" : "KO"
                ))
                .toList();
    }

    // Tickets d'incident iTop ouverts pour le ministere du decideur
    // connecte - section "Incidents" de la page Alertes (cf. endpoint
    // /alertes-reseau ci-dessous pour la section "Alertes reseau" temps
    // reel, calculee depuis NetXMS). Filtre directement cote iTop (cf.
    // ItopClient) : jamais de ticket d'un autre ministere recupere.
    @GetMapping("/incidents")
    public List<TicketIncidentDto> listIncidents() {
        return itopIncidentService.listerAlertesPourMinistere(AccessScopeHolder.getMinistere());
    }

    // Alertes reseau temps reel (changements de statut ANPTIC/LAN,
    // calcules a la volee depuis NetXMS) - section "Alertes reseau" de la
    // page Alertes, distincte des tickets iTop ci-dessus (/incidents).
    // C'est l'ancien contenu de /incidents avant l'integration iTop.
    //
    // Retour utilisateur : affiche desormais TOUS les niveaux (pas
    // seulement les critiques comme precedemment, cf. ancien filtre
    // "item #10" retire) - deja TRIEES par ordre de criticite decroissant
    // (Critique, Majeur, Moyen...) par IncidentService.listIncidents().
    @GetMapping("/alertes-reseau")
    public List<IncidentDto> listAlertesReseau() {
        return filtrerParMinistere(incidentService.listIncidents(), IncidentDto::siteId);
    }

    private void verifierAccesSite(String siteId) {
        // Un profil LAMBDA n'a droit qu'a la consultation du statut global
        // (voir /sites/statut-simple) : aucun endpoint detaille (debit,
        // latence, disponibilite...) ne doit lui etre accessible, meme
        // avec un JWT valide.
        if (AccessScopeHolder.estLambda()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Ce profil n'a pas accès aux données détaillées.");
        }
        String ministere = AccessScopeHolder.getMinistere();
        if (ministere == null) {
            return;
        }
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Site introuvable : " + siteId));
        if (!ministere.equals(site.getMinistere())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce site n'appartient pas à votre ministère.");
        }
    }

    private <T> List<T> filtrerParMinistere(List<T> items, java.util.function.Function<T, String> siteIdExtractor) {
        String ministere = AccessScopeHolder.getMinistere();
        if (ministere == null) {
            return items;
        }
        // Meme regle que listSites() ci-dessus : un site sans equipement
        // n'est pas visible cote decideur, y compris sur la Carte et dans
        // les alertes (ou il n'aurait de toute facon aucun statut
        // ANPTIC/LAN exploitable).
        Set<String> sitesAutorises = avecEquipement(siteRepository.findByActifTrueAndMinistere(ministere)).stream()
                .map(Site::getSiteId)
                .collect(Collectors.toSet());
        return items.stream()
                .filter(item -> sitesAutorises.contains(siteIdExtractor.apply(item)))
                .toList();
    }
    
}