package bf.anptic.geoportail.dto;

public record CartographyItemDto(
        String siteId,
        String nom,
        Double latitude,
        Double longitude,
        String infoAuSurvol,
        boolean positionne,
        String ville,
        String province,
        String regionAdministrative,
        String ministere
) {}