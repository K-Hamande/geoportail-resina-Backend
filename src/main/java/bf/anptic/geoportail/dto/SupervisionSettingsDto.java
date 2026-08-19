package bf.anptic.geoportail.dto;

// Vue Backoffice des parametres de supervision d'un site. "personnalise"
// indique si le site a ses propres valeurs ou s'il utilise les defauts.
public record SupervisionSettingsDto(
        String siteId,
        String siteNom,
        String ville,
        Integer intervalleActualisationS,
        Double debitMinimalMbps,
        Double latenceMaximaleMs,
        Boolean notificationsActives,
        Boolean notifPanneAnptic,
        Boolean notifPanneLan,
        Boolean notifRetablissement,
        boolean personnalise
) {}