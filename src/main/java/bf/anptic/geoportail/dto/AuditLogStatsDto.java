package bf.anptic.geoportail.dto;

import java.util.Map;

// Statistiques de la page Backoffice "Journal d'activite" - calculees sur
// TOUT l'ensemble filtre (texte/auteur), pas seulement la page actuellement
// affichee (meme principe que IncidentHistoriqueStatsDto).
//
// Retour utilisateur du 30/10/2026 : la recherche ne portait auparavant
// que sur les 500 evenements les plus recents (plafond fixe cote
// controleur) - desormais la recherche et ces statistiques portent sur
// TOUT l'historique filtre, avec une vraie pagination pour l'affichage.
public record AuditLogStatsDto(
        long totalFiltre,
        long aujourdHui,
        long septJours,
        long utilisateursActifs,
        // Cle = meme "key" que CATEGORIES_ACTION cote frontend
        // (AuditLogPage.jsx) - suppression/revocation/desactivation/
        // reactivation/activation/creation/reinitialisation/motdepasse/
        // modification/autre. Le libelle/icone/couleur restent definis
        // cote UI, le backend ne renvoie que les compteurs.
        Map<String, Long> parCategorie
) {}
