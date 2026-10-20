package bf.anptic.geoportail.dto;

import java.time.Instant;

// Une ligne de la page Backoffice "Historique des incidents" - onglet
// "Incidents iTop" (section distincte de l'onglet "Alertes reseau",
// cf. IncidentHistoriqueDto).
public record ItopTicketHistoriqueDto(
        Long id,
        String ref,
        String titre,
        String ministere,
        String statut,
        Instant survenuLe,
        Instant clotureLe,
        boolean cloture
) {}
