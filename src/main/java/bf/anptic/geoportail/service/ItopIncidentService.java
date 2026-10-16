package bf.anptic.geoportail.service;

import bf.anptic.geoportail.client.ItopClient;
import bf.anptic.geoportail.dto.TicketIncidentDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class ItopIncidentService {

    private static final Logger log = LoggerFactory.getLogger(ItopIncidentService.class);

    private final ItopClient itopClient;

    public ItopIncidentService(ItopClient itopClient) {
        this.itopClient = itopClient;
    }

    // iTop est un systeme externe : une panne/lenteur de son cote ne doit
    // pas faire planter la page "Alertes" du decideur - on journalise et
    // on renvoie une liste vide plutot que de laisser l'exception remonter.
    public List<TicketIncidentDto> listerAlertesPourMinistere(String ministere) {
        if (ministere == null) return List.of();
        try {
            return itopClient.listerTicketsOuverts(ministere).stream()
                    .sorted(Comparator.comparing(TicketIncidentDto::survenuLe,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
        } catch (Exception e) {
            log.warn("Appel iTop en echec pour le ministere '{}' : {}", ministere, e.getMessage());
            return List.of();
        }
    }
}
