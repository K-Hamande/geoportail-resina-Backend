package bf.anptic.geoportail.controller.backoffice;

import bf.anptic.geoportail.dto.CartographyItemDto;
import bf.anptic.geoportail.dto.CartographyUpdateRequest;
import bf.anptic.geoportail.dto.EquipmentAdminRequest;
import bf.anptic.geoportail.dto.EquipmentResponse;
import bf.anptic.geoportail.dto.SiteAdminRequest;
import bf.anptic.geoportail.dto.SiteAdminResponse;
import bf.anptic.geoportail.service.backoffice.AdminSiteService;
import bf.anptic.geoportail.service.backoffice.NetxmsSiteImportService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/backoffice/api/v1")
public class AdminSiteController {

    private final AdminSiteService adminSiteService;
    private final NetxmsSiteImportService netxmsSiteImportService;

    public AdminSiteController(AdminSiteService adminSiteService,
                                NetxmsSiteImportService netxmsSiteImportService) {
        this.adminSiteService = adminSiteService;
        this.netxmsSiteImportService = netxmsSiteImportService;
    }

    // Synchronise le catalogue des sites depuis netxmsdb (donnebase.siteadministratif,
    // filtre sur connectionresina = 'Oui'). A appeler manuellement depuis le backoffice
    // (bouton "Synchroniser les sites"), pas automatique au demarrage.
    @PostMapping("/sites/import-netxms")
    public NetxmsSiteImportService.ImportResult importSitesFromNetxms() {
        return netxmsSiteImportService.importSitesResina();
    }

    @GetMapping("/sites")
    public List<SiteAdminResponse> listAllSites(Authentication authentication) {
        return adminSiteService.listAllSites(authentication);
    }

    @PostMapping("/sites")
    public SiteAdminResponse createOrUpdateSite(@RequestBody SiteAdminRequest request, Authentication authentication) {
        return adminSiteService.createOrUpdateSite(request, authentication.getName(), authentication);
    }

    @PostMapping("/sites/{siteId}/deactivate")
    public void deactivate(@PathVariable String siteId, Authentication authentication) {
        adminSiteService.setActive(siteId, false, authentication.getName(), authentication);
    }

    @PostMapping("/sites/{siteId}/activate")
    public void activate(@PathVariable String siteId, Authentication authentication) {
        adminSiteService.setActive(siteId, true, authentication.getName(), authentication);
    }

    @GetMapping("/sites/{siteId}/equipments")
    public List<EquipmentResponse> listEquipments(@PathVariable String siteId, Authentication authentication) {
        return adminSiteService.listEquipments(siteId, authentication);
    }

    @PostMapping("/sites/{siteId}/equipments")
    public EquipmentResponse addEquipment(@PathVariable String siteId,
                                           @RequestBody EquipmentAdminRequest request,
                                           Authentication authentication) {
        return adminSiteService.addEquipment(siteId, request, authentication.getName(), authentication);
    }

    @DeleteMapping("/equipments/{equipmentId}")
    public void deleteEquipment(@PathVariable Long equipmentId, Authentication authentication) {
        adminSiteService.deleteEquipment(equipmentId, authentication.getName(), authentication);
    }

    @GetMapping("/cartography")
    public List<CartographyItemDto> listCartography(Authentication authentication) {
        return adminSiteService.listCartography(authentication);
    }

    @PutMapping("/sites/{siteId}/cartography")
    public void updateCartography(@PathVariable String siteId,
                                   @RequestBody CartographyUpdateRequest request,
                                   Authentication authentication) {
        adminSiteService.updateCartography(siteId, request, authentication.getName(), authentication);
    }
}