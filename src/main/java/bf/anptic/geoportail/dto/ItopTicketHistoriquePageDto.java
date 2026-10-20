package bf.anptic.geoportail.dto;

import java.util.List;

// Reponse paginee de GET /backoffice/api/v1/incidents/itop-historique.
public record ItopTicketHistoriquePageDto(
        List<ItopTicketHistoriqueDto> tickets,
        int page,
        int totalPages,
        long totalElements,
        ItopTicketHistoriqueStatsDto stats
) {}
