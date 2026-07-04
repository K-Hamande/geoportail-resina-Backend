package bf.anptic.geoportail.controller.backoffice;

import bf.anptic.geoportail.dto.EquipmentAdminRequest;
import bf.anptic.geoportail.dto.EquipmentResponse;
import bf.anptic.geoportail.dto.CartographyItemDto;
import bf.anptic.geoportail.dto.CartographyUpdateRequest;
import bf.anptic.geoportail.dto.SiteAdminRequest;
import bf.anptic.geoportail.dto.SiteAdminResponse;
import bf.anptic.geoportail.service.backoffice.AdminSiteService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/backoffice/api/v1")
public class AdminSiteController {

    private final AdminSiteService adminSiteService;

    public AdminSiteController(AdminSiteService adminSiteService) {
        this.adminSiteService = adminSiteService;
    }

    @GetMapping("/sites")
    public List<SiteAdminResponse> listAllSites() {
        return adminSiteService.listAllSites();
    }

    @PostMapping("/sites")
    public SiteAdminResponse createOrUpdateSite(@RequestBody SiteAdminRequest request, Authentication authentication) {
        return adminSiteService.createOrUpdateSite(request, authentication.getName());
    }

    @PostMapping("/sites/{siteId}/deactivate")
    public void deactivate(@PathVariable String siteId, Authentication authentication) {
        adminSiteService.setActive(siteId, false, authentication.getName());
    }

    @PostMapping("/sites/{siteId}/activate")
    public void activate(@PathVariable String siteId, Authentication authentication) {
        adminSiteService.setActive(siteId, true, authentication.getName());
    }

    @GetMapping("/sites/{siteId}/equipments")
    public List<EquipmentResponse> listEquipments(@PathVariable String siteId) {
        return adminSiteService.listEquipments(siteId);
    }

    @PostMapping("/sites/{siteId}/equipments")
    public EquipmentResponse addEquipment(@PathVariable String siteId,
                                           @RequestBody EquipmentAdminRequest request,
                                           Authentication authentication) {
        return adminSiteService.addEquipment(siteId, request, authentication.getName());
    }

    @DeleteMapping("/equipments/{equipmentId}")
    public void deleteEquipment(@PathVariable Long equipmentId, Authentication authentication) {
        adminSiteService.deleteEquipment(equipmentId, authentication.getName());
    }


    @GetMapping("/cartography")
    public List<CartographyItemDto> listCartography() {
        return adminSiteService.listCartography();
    }

    @PutMapping("/sites/{siteId}/cartography")
    public void updateCartography(@PathVariable String siteId,
                                   @RequestBody CartographyUpdateRequest request,
                                   Authentication authentication) {
        adminSiteService.updateCartography(siteId, request, authentication.getName());
    }



}