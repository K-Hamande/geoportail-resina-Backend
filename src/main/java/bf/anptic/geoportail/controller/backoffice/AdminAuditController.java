package bf.anptic.geoportail.controller.backoffice;

import bf.anptic.geoportail.dto.AuditLogPageDto;
import bf.anptic.geoportail.service.AuditLogSearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/backoffice/api/v1")
public class AdminAuditController {

    private static final int TAILLE_PAGE_DEFAUT = 30;

    private final AuditLogSearchService auditLogSearchService;

    public AdminAuditController(AuditLogSearchService auditLogSearchService) {
        this.auditLogSearchService = auditLogSearchService;
    }

    // Retour utilisateur du 30/10/2026 : recherche/pagination sur TOUT
    // l'historique (auteur, texte libre sur action/details), plus de
    // plafond fixe a 500 evenements - voir AuditLogSearchService pour le
    // detail du filtrage/calcul des statistiques.
    @GetMapping("/audit-log")
    public AuditLogPageDto getAuditLog(
            @RequestParam(required = false) String auteur,
            @RequestParam(required = false) String recherche,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer taille) {

        int numeroPage = (page != null && page >= 0) ? page : 0;
        int taillePage = (taille != null && taille > 0) ? taille : TAILLE_PAGE_DEFAUT;

        return auditLogSearchService.rechercher(auteur, recherche, numeroPage, taillePage);
    }
}
