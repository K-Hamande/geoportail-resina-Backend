package bf.anptic.geoportail.dto;

public record MinistryAccessTokenCreateRequest(
        String ministere,
        String libelle
) {}