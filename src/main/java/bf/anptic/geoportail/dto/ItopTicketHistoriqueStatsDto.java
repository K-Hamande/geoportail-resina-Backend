package bf.anptic.geoportail.dto;

// Statistiques affichees en haut de l'onglet "Incidents iTop" - calculees
// sur le meme ensemble filtre que la liste (periode/date, ministere,
// recherche), avant pagination : ongoing + resolved + closed == total.
public record ItopTicketHistoriqueStatsDto(
        long total,
        long ongoing,
        long resolved,
        long closed
) {}
