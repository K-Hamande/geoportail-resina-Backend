package bf.anptic.geoportail.service;

import bf.anptic.geoportail.dto.AuditLogPageDto;
import bf.anptic.geoportail.dto.AuditLogStatsDto;
import bf.anptic.geoportail.model.AuditLogEntry;
import bf.anptic.geoportail.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// Retour utilisateur du 30/10/2026 : la page Backoffice "Journal
// d'activite" ne permettait de rechercher/consulter que les 500
// evenements les plus recents (plafond fixe cote controleur, AVANT ce
// service) - impossible de retrouver un evenement plus ancien. Ce
// service recherche/filtre desormais sur TOUT l'historique, et calcule
// les statistiques (aujourd'hui/7 jours/utilisateurs actifs/repartition
// par categorie) sur l'ENSEMBLE filtre, avant de paginer juste la liste
// affichee - meme principe que IncidentHistoryService.
@Service
public class AuditLogSearchService {

    private final AuditLogRepository repository;

    public AuditLogSearchService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public AuditLogPageDto rechercher(String auteur, String recherche, int page, int taillePage) {
        String auteurNormalise = normaliser(auteur);
        String rechercheNormalisee = normaliser(recherche) != null
                ? "%" + recherche.toLowerCase().trim() + "%" : null;

        List<AuditLogEntry> correspondants = repository.rechercher(auteurNormalise, rechercheNormalisee);

        int taillePageEffective = Math.max(1, taillePage);
        int totalElementsFiltres = correspondants.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalElementsFiltres / (double) taillePageEffective));
        int pageEffective = Math.max(0, Math.min(page, totalPages - 1));

        int debutIndex = pageEffective * taillePageEffective;
        int finIndex = Math.min(debutIndex + taillePageEffective, totalElementsFiltres);
        List<AuditLogEntry> contenu = debutIndex < finIndex
                ? correspondants.subList(debutIndex, finIndex)
                : List.of();

        return new AuditLogPageDto(contenu, pageEffective, totalPages, totalElementsFiltres,
                calculerStats(correspondants));
    }

    private AuditLogStatsDto calculerStats(List<AuditLogEntry> correspondants) {
        Instant maintenant = Instant.now();
        Instant debutAujourdHui = maintenant.truncatedTo(ChronoUnit.DAYS);
        Instant ilYA7Jours = maintenant.minus(7, ChronoUnit.DAYS);

        long aujourdHui = correspondants.stream().filter(e -> !e.getHorodatage().isBefore(debutAujourdHui)).count();
        long sept = correspondants.stream().filter(e -> !e.getHorodatage().isBefore(ilYA7Jours)).count();

        Set<String> auteurs = correspondants.stream().map(AuditLogEntry::getAuteur)
                .collect(Collectors.toSet());

        Map<String, Long> parCategorie = new HashMap<>();
        for (AuditLogEntry e : correspondants) {
            parCategorie.merge(categoriser(e.getAction()), 1L, Long::sum);
        }

        return new AuditLogStatsDto(correspondants.size(), aujourdHui, sept, auteurs.size(), parCategorie);
    }

    // Miroir exact de CATEGORIES_ACTION cote frontend (AuditLogPage.jsx) -
    // l'ordre compte : "desactivation"/"reactivation" contiennent tous les
    // deux la sous-chaine "activation", donc testes avant le cas generique.
    private String categoriser(String action) {
        if (action == null) return "autre";
        String a = action.toLowerCase();
        if (a.contains("suppression")) return "suppression";
        if (a.contains("révocation")) return "revocation";
        if (a.contains("désactivation")) return "desactivation";
        if (a.contains("réactivation")) return "reactivation";
        if (a.contains("activation")) return "activation";
        if (a.contains("création")) return "creation";
        if (a.contains("réinitialisation")) return "reinitialisation";
        if (a.contains("mot de passe")) return "motdepasse";
        if (a.contains("modification") || a.contains("coordonnées")) return "modification";
        return "autre";
    }

    private String normaliser(String valeur) {
        return (valeur != null && !valeur.isBlank()) ? valeur.trim() : null;
    }
}
