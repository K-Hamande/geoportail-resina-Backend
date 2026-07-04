package bf.anptic.geoportail.service;

import bf.anptic.geoportail.model.AuditLogEntry;
import bf.anptic.geoportail.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(String auteur, String action, String details) {
        AuditLogEntry entry = new AuditLogEntry();
        entry.setAuteur(auteur);
        entry.setAction(action);
        entry.setDetails(details);
        entry.setHorodatage(Instant.now());
        auditLogRepository.save(entry);
    }
}