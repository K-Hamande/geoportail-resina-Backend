package bf.anptic.geoportail.service.backoffice;

import bf.anptic.geoportail.dto.CartographyItemDto;
import bf.anptic.geoportail.dto.CartographyUpdateRequest;
import bf.anptic.geoportail.dto.EquipmentFloorAssignmentRequest;
import bf.anptic.geoportail.dto.EquipmentResponse;
import bf.anptic.geoportail.dto.SiteAdminRequest;
import bf.anptic.geoportail.dto.SiteAdminResponse;
import bf.anptic.geoportail.model.AdminUser;
import bf.anptic.geoportail.model.Equipment;
import bf.anptic.geoportail.model.Site;
import bf.anptic.geoportail.repository.AdminUserRepository;
import bf.anptic.geoportail.repository.EquipmentRepository;
import bf.anptic.geoportail.repository.SiteRepository;
import bf.anptic.geoportail.service.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminSiteService {

    private final SiteRepository siteRepository;
    private final EquipmentRepository equipmentRepository;
    private final AuditService auditService;
    private final AdminUserRepository adminUserRepository;

    public AdminSiteService(SiteRepository siteRepository,
                             EquipmentRepository equipmentRepository,
                             AuditService auditService,
                             AdminUserRepository adminUserRepository) {
        this.siteRepository = siteRepository;
        this.equipmentRepository = equipmentRepository;
        this.auditService = auditService;
        this.adminUserRepository = adminUserRepository;
    }

    // ---- LECTURE : filtree selon les droits de l'utilisateur connecte ----

    public List<SiteAdminResponse> listAllSites(Authentication authentication) {
        List<Site> sites = filtrerSitesAccessibles(siteRepository.findAll(), authentication);
        Map<String, Integer> comptes = comptesEquipementsParSite();
        return sites.stream().map(site -> toSiteResponse(site, comptes)).toList();
    }

    // Retour utilisateur du 30/10/2026 ("chargement lent") : la page
    // d'edition d'un site (SiteFormPage) recuperait jusqu'ici TOUTE la
    // liste des sites (listAllSites, potentiellement 1600+ entrees) rien
    // que pour en retrouver UN par son id - cout inutile a chaque ouverture
    // de la page. Cette methode ne charge que le site demande.
    public SiteAdminResponse getSite(String siteId, Authentication authentication) {
        verifierAccesSite(siteId, authentication);
        return toSiteResponse(findSiteOrThrow(siteId));
    }

    public List<EquipmentResponse> listEquipments(String siteId, Authentication authentication) {
        verifierAccesSite(siteId, authentication);
        return equipmentRepository.findBySite_SiteId(siteId).stream()
                .map(this::toEquipmentResponse)
                .toList();
    }

    public List<CartographyItemDto> listCartography(Authentication authentication) {
        List<Site> sites = filtrerSitesAccessibles(siteRepository.findAll(), authentication);
        Map<String, Integer> comptes = comptesEquipementsParSite();
        return sites.stream()
                .map(site -> new CartographyItemDto(
                        site.getSiteId(), site.getNom(), site.getLatitude(), site.getLongitude(),
                        site.getInfoAuSurvol(), site.getLatitude() != null && site.getLongitude() != null,
                        site.getVille(), site.getProvince(), site.getRegionAdministrative(), site.getMinistere(),
                        comptes.getOrDefault(site.getSiteId(), 0)))
                .toList();
    }

    // ---- ECRITURE : verifiee au cas par cas ----

    public SiteAdminResponse createOrUpdateSite(SiteAdminRequest request, String auteur, Authentication authentication) {
        verifierAccesSite(request.siteId(), authentication);

        boolean isNew = siteRepository.findById(request.siteId()).isEmpty();
        Site site = siteRepository.findById(request.siteId()).orElseGet(Site::new);

        site.setSiteId(request.siteId());
        site.setNom(request.nom());
        site.setVille(request.ville());
        site.setRegionAdministrative(request.regionAdministrative());
        site.setBatiment(request.batiment());
        site.setLatitude(request.latitude());
        site.setLongitude(request.longitude());
        site.setContactDsiNom(request.contactDsiNom());
        site.setContactDsiTelephone(request.contactDsiTelephone());
        site.setContactDsiEmail(request.contactDsiEmail());
        site.setNetxmsNodeId(request.netxmsNodeId());
        site.setNiveaux(request.niveaux());
        if (request.ministere() != null) site.setMinistere(request.ministere());
        if (request.province() != null) site.setProvince(request.province());

        if (site.getActif() == null) {
            site.setActif(true);
        }

        Site saved = siteRepository.save(site);
        auditService.record(auteur, isNew ? "Création site" : "Modification site",
                "site=" + saved.getSiteId() + " (" + saved.getNom() + ")");

        return toSiteResponse(saved);
    }


    public void setActive(String siteId, boolean active, String auteur, Authentication authentication) {
        verifierAccesSite(siteId, authentication);

        Site site = findSiteOrThrow(siteId);
        site.setActif(active);
        siteRepository.save(site);

        auditService.record(auteur, active ? "Réactivation site" : "Désactivation site", "site=" + siteId);
    }

    // Les equipements sont decouverts automatiquement depuis NetXMS
    // (EquipmentSyncService) - le backoffice ne peut plus qu'assigner
    // un etage a un equipement deja synchronise.
    public EquipmentResponse assignFloor(Long equipmentId, EquipmentFloorAssignmentRequest request, String auteur, Authentication authentication) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Équipement introuvable : " + equipmentId));

        verifierAccesSite(equipment.getSite().getSiteId(), authentication);

        equipment.setEtageLabel(request.etageLabel());
        equipment.setLibelleAffiche(request.libelleAffiche());
        Equipment saved = equipmentRepository.save(equipment);

        auditService.record(auteur, "Modification équipement",
                "equipmentId=" + equipmentId + " étage=" + request.etageLabel() + " libellé=" + request.libelleAffiche());

        return toEquipmentResponse(saved);
    }

    public void updateCartography(String siteId, CartographyUpdateRequest request, String auteur, Authentication authentication) {
        verifierAccesSite(siteId, authentication);

        Site site = findSiteOrThrow(siteId);
        site.setLatitude(request.latitude());
        site.setLongitude(request.longitude());
        site.setInfoAuSurvol(request.infoAuSurvol());
        siteRepository.save(site);

        auditService.record(auteur, "Modification coordonnées GPS",
                "site=" + siteId + " lat=" + request.latitude() + " lon=" + request.longitude());
    }

    // ---- Utilitaires de controle d'acces (§3.2.6b : droits par site ou globaux) ----

    /**
     * Ne garde, dans la liste fournie, que les sites accessibles a
     * l'utilisateur connecte (tous, si SUPER_ADMIN ou aucune restriction
     * definie sur son compte).
     */
    private List<Site> filtrerSitesAccessibles(List<Site> sites, Authentication authentication) {
        boolean superAdmin = estSuperAdmin(authentication);
        if (superAdmin) {
            return sites;
        }

        AdminUser user = adminUserRepository.findByLoginAndActifTrue(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Compte introuvable"));

        if (user.aAccesGlobal()) {
            return sites;
        }

        return sites.stream()
                .filter(site -> user.peutAccederAuSite(site.getSiteId()))
                .toList();
    }

    /** Leve une 403 si l'utilisateur connecte n'a pas le droit d'agir sur ce site. */
    private void verifierAccesSite(String siteId, Authentication authentication) {
        if (estSuperAdmin(authentication)) {
            return;
        }

        AdminUser user = adminUserRepository.findByLoginAndActifTrue(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Compte introuvable"));

        if (!user.peutAccederAuSite(siteId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Vous n'avez pas les droits sur le site : " + siteId);
        }
    }

    private boolean estSuperAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
    }

    private Site findSiteOrThrow(String siteId) {
        return siteRepository.findById(siteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Site introuvable : " + siteId));
    }

    // Pour UN site isole (lecture/ecriture unitaire) - cout acceptable ici,
    // une seule requete de comptage.
    private SiteAdminResponse toSiteResponse(Site site) {
        int nombreEquipements = (int) equipmentRepository.countBySite_SiteId(site.getSiteId());
        return toSiteResponse(site, Map.of(site.getSiteId(), nombreEquipements));
    }

    // Pour une LISTE de sites (listAllSites) - le compte d'equipements est
    // precalcule en une seule requete groupee (cf. comptesEquipementsParSite)
    // plutot que rappelee ici site par site.
    private SiteAdminResponse toSiteResponse(Site site, Map<String, Integer> comptesParSite) {
        int nombreEquipements = comptesParSite.getOrDefault(site.getSiteId(), 0);
        return new SiteAdminResponse(
                site.getSiteId(), site.getNom(), site.getVille(), site.getProvince(), site.getRegionAdministrative(),
                site.getMinistere(), site.getBatiment(),
                site.getLatitude(), site.getLongitude(), site.getContactDsiNom(), site.getContactDsiTelephone(),
                site.getContactDsiEmail(), site.getNetxmsNodeId(), site.getNiveaux(), nombreEquipements, site.getActif()
        );
    }

    // Retour utilisateur du 30/10/2026 : une seule requete groupee pour
    // TOUS les sites plutot qu'une requete de comptage par site (ancien
    // comportement, cf. countBySite_SiteId ci-dessus) - voir le
    // commentaire sur EquipmentRepository.countGroupedBySiteId.
    private Map<String, Integer> comptesEquipementsParSite() {
        Map<String, Integer> resultat = new HashMap<>();
        for (Object[] ligne : equipmentRepository.countGroupedBySiteId()) {
            resultat.put((String) ligne[0], ((Long) ligne[1]).intValue());
        }
        return resultat;
    }

    private EquipmentResponse toEquipmentResponse(Equipment eq) {
        return new EquipmentResponse(
                eq.getId(), eq.getSite().getSiteId(), eq.getEtageLabel(),
                eq.getType() != null ? eq.getType().name() : null,
                eq.getLibelleAffiche(), eq.getNomTechniqueNetxms(), eq.getNetxmsObjectId()
        );
    }
}