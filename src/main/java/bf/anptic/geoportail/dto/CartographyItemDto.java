package bf.anptic.geoportail.dto;

// Renvoye cote BACKOFFICE : vue d'ensemble du positionnement de chaque site.
public record CartographyItemDto(
        String siteId,
        String nom,
        Double latitude,
        Double longitude,
        String infoAuSurvol,
        boolean positionne   // true si latitude ET longitude sont renseignees
) {}