package bf.anptic.geoportail.dto;

// Recu depuis le Backoffice pour positionner un site sur la carte (§3.2.6a).
public record CartographyUpdateRequest(
        Double latitude,
        Double longitude,
        String infoAuSurvol
) {}