package bf.anptic.geoportail.controller.backoffice;

import bf.anptic.geoportail.model.AuditLogEntry;
import bf.anptic.geoportail.repository.AuditLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/backoffice/api/v1")
public class AdminAuditController {

    // Limite de securite : le journal peut grandir indefiniment avec le
    // temps ; on ne renvoie que les 500 evenements les plus recents a
    // l'affichage plutot que l'historique complet (l'export CSV, lui,
    // reste base sur ce qui est charge - a etendre plus tard avec un
    // vrai export cote serveur si besoin d'un historique complet).
    private static final int LIMITE_AFFICHAGE = 500;

    private final AuditLogRepository auditLogRepository;

    public AdminAuditController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/audit-log")
    public List<AuditLogEntry> getAuditLog() {
        return auditLogRepository
                .findAll(PageRequest.of(0, LIMITE_AFFICHAGE, Sort.by(Sort.Direction.DESC, "horodatage")))
                .getContent();
    }
}