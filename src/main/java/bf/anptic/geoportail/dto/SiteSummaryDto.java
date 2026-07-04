package bf.anptic.geoportail.dto;

// Utilise par /api/v1/sites (cote DECIDEUR, §3.2.3) : le strict minimum
// pour afficher une liste de selection, sans exposer de details internes.
public record SiteSummaryDto(
        String siteId,
        String nom,
        String ville
) {}