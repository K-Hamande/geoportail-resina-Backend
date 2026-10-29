package bf.anptic.geoportail.service;

import bf.anptic.geoportail.client.ItopClient;
import bf.anptic.geoportail.client.ItopClient.TicketBrut;
import bf.anptic.geoportail.dto.ItopTicketHistoriqueDto;
import bf.anptic.geoportail.dto.ItopTicketHistoriquePageDto;
import bf.anptic.geoportail.dto.ItopTicketHistoriqueStatsDto;
import bf.anptic.geoportail.model.ItopTicketHistorique;
import bf.anptic.geoportail.repository.ItopTicketHistoriqueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Equivalent de IncidentHistoryService, mais pour les tickets iTop : au
// lieu de DETECTER des transitions d'etat depuis un calcul temps reel
// (comme pour les alertes reseau ANPTIC/LAN), on se contente de
// RECOPIER localement ce qu'iTop connait deja (il porte nativement ses
// propres dates de debut/cloture) - plus simple, pas de logique
// d'ouverture/fermeture a gerer cote GeoPortail.
@Service
public class ItopTicketHistoryService {

    private static final Logger log = LoggerFactory.getLogger(ItopTicketHistoryService.class);

    // Plafond de securite sur une synchronisation COMPLETE (table encore
    // vide) - evite un payload demesure si le nombre de tickets iTop
    // grossit beaucoup ; les synchronisations suivantes sont incrementales
    // (cf. synchroniser()) et n'ont pas besoin de ce plafond en pratique.
    private static final int LIMITE_SYNC_COMPLETE = 10000;

    private final ItopClient itopClient;
    private final ItopTicketHistoriqueRepository repository;

    public ItopTicketHistoryService(ItopClient itopClient, ItopTicketHistoriqueRepository repository) {
        this.itopClient = itopClient;
        this.repository = repository;
    }

    // A appeler periodiquement (cf. ItopSyncScheduler). iTop etant un
    // systeme externe, une panne/lenteur de son cote ne doit pas faire
    // planter le job - on journalise et on abandonne ce passage, la
    // prochaine execution reessaiera.
    @Transactional
    public void synchroniser() {
        Instant derniereMajConnue = repository.trouverDerniereMajConnue();
        List<TicketBrut> tickets;
        try {
            tickets = itopClient.listerTousLesTickets(derniereMajConnue, LIMITE_SYNC_COMPLETE);
        } catch (Exception e) {
            log.warn("Synchronisation iTop en echec : {}", e.getMessage());
            return;
        }

        for (TicketBrut ticket : tickets) {
            if (ticket.ref() == null) continue;
            ItopTicketHistorique enregistrement = repository.findByRef(ticket.ref()).orElseGet(ItopTicketHistorique::new);
            enregistrement.setRef(ticket.ref());
            enregistrement.setTitre(ticket.titre());
            enregistrement.setMinistere(ticket.ministere());
            enregistrement.setStatut(ticket.statut());
            enregistrement.setSurvenuLe(ticket.survenuLe() != null ? ticket.survenuLe() : Instant.now());
            enregistrement.setClotureLe(ticket.clotureLe());
            enregistrement.setDerniereMajItop(ticket.derniereMaj());
            repository.save(enregistrement);
        }

        if (!tickets.isEmpty()) {
            log.info("Synchronisation iTop : {} ticket(s) mis a jour", tickets.size());
        }
    }

    // Pour la page Backoffice "Historique des incidents" - onglet
    // "Incidents iTop". Meme principe que IncidentHistoryService.rechercher :
    // fenetre/ministere/recherche filtres en base, statut/tri/pagination en
    // memoire (volume attendu raisonnable).
    @Transactional(readOnly = true)
    public ItopTicketHistoriquePageDto rechercher(Instant fenetreDebut, Instant fenetreFin,
                                                   String statut, String ministere, String recherche,
                                                   int page, int taillePage) {
        ResultatFiltrage resultat = filtrerEtTrier(fenetreDebut, fenetreFin, statut, ministere, recherche);
        List<ItopTicketHistorique> filtres = resultat.filtresParStatut();

        int taillePageEffective = Math.max(1, taillePage);
        int totalElementsFiltres = filtres.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalElementsFiltres / (double) taillePageEffective));
        int pageEffective = Math.max(0, Math.min(page, totalPages - 1));

        int debutIndex = pageEffective * taillePageEffective;
        int finIndex = Math.min(debutIndex + taillePageEffective, totalElementsFiltres);
        List<ItopTicketHistoriqueDto> contenu = debutIndex < finIndex
                ? filtres.subList(debutIndex, finIndex).stream()
                        .map(t -> toDto(t, resultat.nombreTicketsParMinistere()))
                        .toList()
                : List.of();

        return new ItopTicketHistoriquePageDto(
                contenu, pageEffective, totalPages, totalElementsFiltres,
                new ItopTicketHistoriqueStatsDto(resultat.total(), resultat.ongoing(), resultat.resolved(), resultat.closed())
        );
    }

    @Transactional(readOnly = true)
    public List<ItopTicketHistoriqueDto> rechercherPourExport(Instant fenetreDebut, Instant fenetreFin,
                                                                String statut, String ministere, String recherche) {
        ResultatFiltrage resultat = filtrerEtTrier(fenetreDebut, fenetreFin, statut, ministere, recherche);
        return resultat.filtresParStatut().stream()
                .map(t -> toDto(t, resultat.nombreTicketsParMinistere()))
                .toList();
    }

    private record ResultatFiltrage(List<ItopTicketHistorique> filtresParStatut,
                                     long total, long ongoing, long resolved, long closed,
                                     Map<String, Integer> nombreTicketsParMinistere) {
    }

    private ResultatFiltrage filtrerEtTrier(Instant fenetreDebut, Instant fenetreFin,
                                             String statut, String ministere, String recherche) {
        // En minuscules ici (pas de LOWER() cote SQL sur ce parametre -
        // cf. BUG CORRIGE dans ItopTicketHistoriqueRepository.rechercher).
        String ministereNormalise = normaliser(ministere) != null ? ministere.toLowerCase().trim() : null;
        String rechercheNormalisee = normaliser(recherche) != null
                ? "%" + recherche.toLowerCase().trim() + "%" : null;

        List<ItopTicketHistorique> correspondants =
                repository.rechercher(fenetreDebut, fenetreFin, ministereNormalise, rechercheNormalisee);

        long total = correspondants.size();
        long ongoing = correspondants.stream().filter(t -> "ongoing".equals(t.getStatut())).count();
        long resolved = correspondants.stream().filter(t -> "resolved".equals(t.getStatut())).count();
        long closed = correspondants.stream().filter(t -> "closed".equals(t.getStatut())).count();

        List<ItopTicketHistorique> filtresParStatut = correspondants.stream()
                .filter(t -> statut == null || statut.isBlank() || statut.equals(t.getStatut()))
                .sorted(Comparator.comparing(ItopTicketHistorique::getSurvenuLe, Comparator.reverseOrder()))
                .toList();

        // Section V du compte rendu des amendements : nombre de tickets
        // par ministere sur cette meme fenetre/ces memes filtres - pas de
        // Collectors.groupingBy direct (NPE si ministere est null, ce qui
        // arrive pour certains tickets iTop).
        Map<String, Integer> nombreTicketsParMinistere = new HashMap<>();
        for (ItopTicketHistorique t : correspondants) {
            String cle = t.getMinistere() != null ? t.getMinistere() : "";
            nombreTicketsParMinistere.merge(cle, 1, Integer::sum);
        }

        return new ResultatFiltrage(filtresParStatut, total, ongoing, resolved, closed, nombreTicketsParMinistere);
    }

    private String normaliser(String valeur) {
        return (valeur != null && !valeur.isBlank()) ? valeur.trim() : null;
    }

    private ItopTicketHistoriqueDto toDto(ItopTicketHistorique t, Map<String, Integer> nombreTicketsParMinistere) {
        String cle = t.getMinistere() != null ? t.getMinistere() : "";
        return new ItopTicketHistoriqueDto(
                t.getId(), t.getRef(), t.getTitre(), t.getMinistere(), t.getStatut(),
                t.getSurvenuLe(), t.getClotureLe(), t.getClotureLe() != null,
                nombreTicketsParMinistere.getOrDefault(cle, 0)
        );
    }
}
