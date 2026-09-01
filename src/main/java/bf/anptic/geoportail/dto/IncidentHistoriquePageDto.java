package bf.anptic.geoportail.dto;

import java.util.List;

// Reponse paginee de GET /backoffice/api/v1/incidents/historique.
public record IncidentHistoriquePageDto(
        List<IncidentHistoriqueDto> incidents,
        int page,
        int totalPages,
        long totalElements,
        IncidentHistoriqueStatsDto stats
) {}