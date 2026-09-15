package bf.anptic.geoportail.dto;

// Bandeau de statistiques de la page publique "Geoportail national"
// (couverture RESINA par commune).
public record CouvertureStatsDto(
        long totalCommunes,
        long communesConnectees,
        long sitesRaccordes,
        double kmLiaisons
) {}
