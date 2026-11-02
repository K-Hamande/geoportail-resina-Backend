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
        String ministere,
        // Retour utilisateur du 30/10/2026 : filtre "sites avec au moins un
        // equipement" sur la page Cartographie, meme principe que
        // SiteAdminResponse.nombreEquipements deja utilise par SitesPage.
        int nombreEquipements
) {}