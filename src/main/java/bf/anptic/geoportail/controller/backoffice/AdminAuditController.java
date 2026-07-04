package bf.anptic.geoportail.controller.backoffice;

import bf.anptic.geoportail.model.AuditLogEntry;
import bf.anptic.geoportail.repository.AuditLogRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/backoffice/api/v1")
public class AdminAuditController {

    private final AuditLogRepository auditLogRepository;

    public AdminAuditController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/audit-log")
    public List<AuditLogEntry> getAuditLog() {
        return auditLogRepository.findAllByOrderByHorodatageDesc();
    }
}