package bf.anptic.geoportail.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Alimente periodiquement la copie locale des tickets iTop
// (itop_ticket_historique) utilisee par la page Backoffice "Historique
// des incidents" - onglet "Incidents iTop". Separe de
// IncidentAlertScheduler (NetXMS) : source de donnees, cadence et echecs
// possibles totalement independants, pas de raison de les coupler.
@Component
public class ItopSyncScheduler {

    private final ItopTicketHistoryService itopTicketHistoryService;

    public ItopSyncScheduler(ItopTicketHistoryService itopTicketHistoryService) {
        this.itopTicketHistoryService = itopTicketHistoryService;
    }

    @Scheduled(cron = "${resina.itop.sync-cron:0 */10 * * * *}")
    public void synchroniser() {
        itopTicketHistoryService.synchroniser();
    }
}
