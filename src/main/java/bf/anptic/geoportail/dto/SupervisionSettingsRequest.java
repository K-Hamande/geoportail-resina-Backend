package bf.anptic.geoportail.dto;

public record SupervisionSettingsRequest(
        Integer intervalleActualisationS,
        Double debitMinimalMbps,
        Double latenceMaximaleMs,
        Boolean notificationsActives,
        Boolean notifPanneAnptic,
        Boolean notifPanneLan,
        Boolean notifRetablissement
) {}