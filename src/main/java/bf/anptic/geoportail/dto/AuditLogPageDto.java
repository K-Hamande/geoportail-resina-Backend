package bf.anptic.geoportail.dto;

import bf.anptic.geoportail.model.AuditLogEntry;

import java.util.List;

public record AuditLogPageDto(
        List<AuditLogEntry> entries,
        int page,
        int totalPages,
        long totalElements,
        AuditLogStatsDto stats
) {}
