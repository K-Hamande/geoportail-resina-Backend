package bf.anptic.geoportail.service;

import bf.anptic.geoportail.dto.IncidentDto;
import bf.anptic.geoportail.dto.IncidentHistoriqueDto;
import bf.anptic.geoportail.dto.IncidentHistoriquePageDto;
import bf.anptic.geoportail.dto.IncidentHistoriqueStatsDto;
import bf.anptic.geoportail.model.IncidentHistorique;
import bf.anptic.geoportail.model.Site;
import bf.anptic.geoportail.repository.IncidentHistoriqueRepository;
import bf.anptic.geoportail.repository.SiteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// Fait le pont entre l'etat "temps reel" (IncidentService, recalcule a
// chaque appel depuis NetXMS) et une VRAIE table d'historique
// (incident_historique) : ouvre un enregistrement des qu'un incident
// apparait, le met a jour tant qu'il reste actif (le statut peut
// s'aggraver WARN -> KO), et pose une date de fin des qu'il n'est plus
// actif. C'est aussi la source de verite utilisee par IncidentAlertScheduler
// pour savoir si un incident est VRAIMENT nouveau (voir sa Javadoc) - plus
// fiable qu'un simple Set en memoire, qui oubliait tout au redemarrage du
// serveur.
@Service
public class IncidentHistoryService {

    private final IncidentService incidentService;
    private final IncidentHistoriqueRepository repository;
    private final SiteRepository siteRepository;

    public IncidentHistoryService(IncidentService incidentService, IncidentHistoriqueRepository repository,
                                   SiteRepository siteRepository) {
        this.incidentService = incidentService;
        this.siteRepository = siteRepository;
        this.repository = repository;
    }

    // A appeler periodiquement (voir IncidentAlertScheduler, qui porte le
    // @Scheduled - un seul point d'appel pour eviter d'interroger NetXMS
    // deux fois en parallele). Retourne a la fois les incidents qui
    // viennent d'OUVRIR un nouvel enregistrement (nouveaux) et ceux qui
    // viennent de se refermer (resolus) a cet appel precis - les deux
    // alimentent les notifications (email + push, cf. IncidentAlertScheduler
    // et les reglages notifPanneAnptic/notifPanneLan/notifRetablissement de
    // SiteSupervisionSettings).
    @Transactional
    public DetectionResultat detecterEtEnregistrer() {
        List<IncidentDto> actifs = incidentService.listIncidents();
        Set<String> clesActives = actifs.stream().map(IncidentDto::id).collect(Collectors.toSet());
        List<IncidentDto> nouveaux = new ArrayList<>();

        for (IncidentDto incident : actifs) {
            IncidentHistorique existant = repository.findByIncidentKeyAndFinLeIsNull(incident.id()).orElse(null);

            if (existant == null) {
                IncidentHistorique enregistrement = new IncidentHistorique();
                enregistrement.setIncidentKey(incident.id());
                enregistrement.setType(incident.type());
                enregistrement.setSiteId(incident.siteId());
                enregistrement.setSiteNom(incident.siteNom());
                enregistrement.setVille(incident.ville());
                enregistrement.setMinistere(incident.ministere());
                enregistrement.setStatut(incident.nouveauStatut().name());
                enregistrement.setMessage(incident.message());
                enregistrement.setDebutLe(incident.survenuLe());
                repository.save(enregistrement);
                nouveaux.add(incident);
            } else if (!incident.nouveauStatut().name().equals(existant.getStatut())
                    || !incident.message().equals(existant.getMessage())) {
                // Toujours actif, mais le statut ou le message a change
                // (ex : WARN -> KO, ou le nombre d'equipements en panne a
                // evolue) - on rafraichit SANS toucher a debutLe.
                existant.setStatut(incident.nouveauStatut().name());
                existant.setMessage(incident.message());
                repository.save(existant);
            }
        }

        Instant maintenant = Instant.now();
        List<IncidentHistorique> resolus = new ArrayList<>();
        for (IncidentHistorique ouvert : repository.findAllByFinLeIsNull()) {
            if (!clesActives.contains(ouvert.getIncidentKey())) {
                ouvert.setFinLe(maintenant);
                repository.save(ouvert);
                resolus.add(ouvert);
            }
        }

        return new DetectionResultat(nouveaux, resolus);
    }

    public record DetectionResultat(List<IncidentDto> nouveaux, List<IncidentHistorique> resolus) {}

    // Pour la page Backoffice "Historique des incidents" : filtres
    // (fenetre de dates, type, etat, ministere, recherche texte),
    // statistiques et pagination. Le filtrage/tri/pagination se fait en
    // memoire plutot qu'en SQL (comme deja fait pour le tri dans l'ancienne
    // version) - volume attendu raisonnable (quelques milliers de lignes
    // au pire sur "tout l'historique"), et ca permet de calculer les
    // statistiques sur l'ensemble filtre AVANT de paginer sans requete
    // supplementaire.
    @Transactional(readOnly = true)
    public IncidentHistoriquePageDto rechercher(Instant fenetreDebut, Instant fenetreFin,
                                                 String type, String etat, String ministere, String recherche,
                                                 int page, int taillePage) {
        ResultatFiltrage resultat = filtrerEtTrier(fenetreDebut, fenetreFin, type, etat, ministere, recherche);
        List<IncidentHistorique> filtresParEtat = resultat.filtresParEtat();

        int taillePageEffective = Math.max(1, taillePage);
        int totalElementsFiltres = filtresParEtat.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalElementsFiltres / (double) taillePageEffective));
        int pageEffective = Math.max(0, Math.min(page, totalPages - 1));

        int debutIndex = pageEffective * taillePageEffective;
        int finIndex = Math.min(debutIndex + taillePageEffective, totalElementsFiltres);
        List<IncidentHistoriqueDto> contenu = debutIndex < finIndex
                ? filtresParEtat.subList(debutIndex, finIndex).stream()
                        .map(i -> toDto(i, resultat.nombreIncidentsParSite(), resultat.disponibiliteParSite()))
                        .toList()
                : List.of();

        return new IncidentHistoriquePageDto(
                contenu, pageEffective, totalPages, totalElementsFiltres,
                new IncidentHistoriqueStatsDto(resultat.total(), resultat.resolus(), resultat.enCours(),
                        resultat.ko(), resultat.warn(), resultat.disponibilitePct(), resultat.indisponibilitePct())
        );
    }

    // Meme filtrage/tri que rechercher(...), mais SANS pagination - pour
    // l'export CSV (voir AdminIncidentController /historique/export), qui
    // doit inclure toutes les lignes correspondant aux filtres actifs sur
    // la page, pas seulement la page actuellement affichee.
    @Transactional(readOnly = true)
    public List<IncidentHistoriqueDto> rechercherPourExport(Instant fenetreDebut, Instant fenetreFin,
                                                             String type, String etat, String ministere,
                                                             String recherche) {
        ResultatFiltrage resultat = filtrerEtTrier(fenetreDebut, fenetreFin, type, etat, ministere, recherche);
        return resultat.filtresParEtat().stream()
                .map(i -> toDto(i, resultat.nombreIncidentsParSite(), resultat.disponibiliteParSite()))
                .toList();
    }

    private record ResultatFiltrage(List<IncidentHistorique> filtresParEtat,
                                     long total, long resolus, long enCours, long ko, long warn,
                                     Double disponibilitePct, Double indisponibilitePct,
                                     Map<String, Integer> nombreIncidentsParSite,
                                     Map<String, Double> disponibiliteParSite) {
    }

    private ResultatFiltrage filtrerEtTrier(Instant fenetreDebut, Instant fenetreFin,
                                             String type, String etat, String ministere, String recherche) {
        String typeNormalise = normaliser(type);
        String ministereNormalise = normaliser(ministere);
        String rechercheNormalisee = normaliser(recherche) != null
                ? "%" + recherche.toLowerCase().trim() + "%" : null;

        List<IncidentHistorique> correspondants =
                repository.rechercher(fenetreDebut, fenetreFin, typeNormalise, ministereNormalise, rechercheNormalisee);

        long total = correspondants.size();
        long enCours = correspondants.stream().filter(i -> i.getFinLe() == null).count();
        long resolus = total - enCours;
        long ko = correspondants.stream().filter(i -> "KO".equals(i.getStatut())).count();
        long warn = correspondants.stream().filter(i -> "WARN".equals(i.getStatut())).count();

        List<IncidentHistorique> filtresParEtat = correspondants.stream()
                .filter(i -> {
                    if ("EN_COURS".equals(etat)) return i.getFinLe() == null;
                    if ("RESOLU".equals(etat)) return i.getFinLe() != null;
                    return true;
                })
                .sorted(Comparator
                        .comparing((IncidentHistorique i) -> i.getFinLe() != null)
                        .thenComparing(IncidentHistorique::getDebutLe, Comparator.reverseOrder()))
                .toList();

        Double[] disponibilite = calculerDisponibilite(correspondants, fenetreDebut, fenetreFin, ministereNormalise);

        Map<String, List<IncidentHistorique>> correspondantsParSite = correspondants.stream()
                .collect(Collectors.groupingBy(IncidentHistorique::getSiteId));
        Map<String, Integer> nombreIncidentsParSite = new HashMap<>();
        Map<String, Double> disponibiliteParSite = new HashMap<>();
        long dureeFenetreMinutes = Duration.between(fenetreDebut, fenetreFin).toMinutes();
        for (Map.Entry<String, List<IncidentHistorique>> entree : correspondantsParSite.entrySet()) {
            nombreIncidentsParSite.put(entree.getKey(), entree.getValue().size());
            disponibiliteParSite.put(entree.getKey(),
                    calculerDisponibiliteSite(entree.getValue(), fenetreDebut, fenetreFin, dureeFenetreMinutes));
        }

        return new ResultatFiltrage(filtresParEtat, total, resolus, enCours, ko, warn,
                disponibilite[0], disponibilite[1], nombreIncidentsParSite, disponibiliteParSite);
    }

    // Meme principe que calculerDisponibilite ci-dessous, mais pour UN
    // SEUL site (celui de la ligne affichee) plutot que pour l'ensemble
    // du parc filtre par ministere - alimente les colonnes "Disponibilite"
    // et "Nombre d'incidents" du tableau (section V du compte rendu des
    // amendements).
    private Double calculerDisponibiliteSite(List<IncidentHistorique> incidentsDuSite,
                                               Instant fenetreDebut, Instant fenetreFin, long dureeFenetreMinutes) {
        if (dureeFenetreMinutes <= 0) {
            return null;
        }
        long tempsIndisponibleMinutes = incidentsDuSite.stream()
                .mapToLong(i -> {
                    Instant debutTronque = i.getDebutLe().isBefore(fenetreDebut) ? fenetreDebut : i.getDebutLe();
                    Instant finIncident = i.getFinLe() != null ? i.getFinLe() : Instant.now();
                    Instant finTronquee = finIncident.isAfter(fenetreFin) ? fenetreFin : finIncident;
                    long minutes = Duration.between(debutTronque, finTronquee).toMinutes();
                    return Math.max(0, minutes);
                })
                .sum();
        double indisponibilitePct = Math.min(100.0, (tempsIndisponibleMinutes * 100.0) / dureeFenetreMinutes);
        return Math.round((100.0 - indisponibilitePct) * 10) / 10.0;
    }

    // Section V du compte rendu des amendements : taux de disponibilite/
    // indisponibilite sur la fenetre de dates FILTREE (pas 30 jours fixes
    // comme AnpticStatusDto.disponibilite30Jours, qui lui est cote decideur
    // et par site). Principe : population totale de "temps-site" possible
    // sur la fenetre = (nombre de sites actifs concernes) x (duree de la
    // fenetre), dont on retranche le temps d'indisponibilite cumule
    // (chaque incident ANPTIC/LAN de la fenetre, tronque aux bornes de
    // celle-ci pour ne pas compter de temps hors fenetre).
    //
    // Choix assume : la population de sites est filtree UNIQUEMENT par
    // ministere (pas par type ni recherche texte) - ces 2 derniers filtres
    // affinent la LISTE d'incidents affichee, mais redefinir la population
    // totale de sites a chaque recherche texte rendrait le taux difficile
    // a interpreter ("disponibilite de quoi, exactement ?"). Avec un
    // filtre ministere actif, c'est la disponibilite des sites de CE
    // ministere ; sans filtre, celle de tout le reseau actif.
    private Double[] calculerDisponibilite(List<IncidentHistorique> correspondants,
                                            Instant fenetreDebut, Instant fenetreFin, String ministereNormalise) {
        long dureeFenetreMinutes = Duration.between(fenetreDebut, fenetreFin).toMinutes();
        if (dureeFenetreMinutes <= 0) {
            return new Double[]{null, null};
        }

        List<Site> sites = ministereNormalise != null
                ? siteRepository.findByActifTrueAndMinistere(ministereNormalise)
                : siteRepository.findByActifTrue();
        if (sites.isEmpty()) {
            return new Double[]{null, null};
        }

        long tempsPossibleMinutes = (long) sites.size() * dureeFenetreMinutes;

        long tempsIndisponibleMinutes = correspondants.stream()
                .mapToLong(i -> {
                    Instant debutTronque = i.getDebutLe().isBefore(fenetreDebut) ? fenetreDebut : i.getDebutLe();
                    Instant finIncident = i.getFinLe() != null ? i.getFinLe() : Instant.now();
                    Instant finTronquee = finIncident.isAfter(fenetreFin) ? fenetreFin : finIncident;
                    long minutes = Duration.between(debutTronque, finTronquee).toMinutes();
                    return Math.max(0, minutes);
                })
                .sum();

        double indisponibilitePct = Math.min(100.0, (tempsIndisponibleMinutes * 100.0) / tempsPossibleMinutes);
        double disponibilitePct = 100.0 - indisponibilitePct;

        return new Double[]{
                Math.round(disponibilitePct * 10) / 10.0,
                Math.round(indisponibilitePct * 10) / 10.0
        };
    }

    private String normaliser(String valeur) {
        return (valeur != null && !valeur.isBlank()) ? valeur.trim() : null;
    }

    private IncidentHistoriqueDto toDto(IncidentHistorique i, Map<String, Integer> nombreIncidentsParSite,
                                         Map<String, Double> disponibiliteParSite) {
        Instant fin = i.getFinLe();
        long dureeMinutes = Duration.between(i.getDebutLe(), fin != null ? fin : Instant.now()).toMinutes();
        return new IncidentHistoriqueDto(
                i.getId(), i.getType(), i.getSiteId(), i.getSiteNom(), i.getVille(), i.getMinistere(),
                i.getStatut(), i.getMessage(), i.getDebutLe(), fin, fin == null, dureeMinutes,
                nombreIncidentsParSite.getOrDefault(i.getSiteId(), 0),
                disponibiliteParSite.get(i.getSiteId())
        );
    }
}