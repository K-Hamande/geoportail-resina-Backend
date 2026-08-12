package bf.anptic.geoportail.controller.backoffice;

import bf.anptic.geoportail.dto.CartographyItemDto;
import bf.anptic.geoportail.dto.CartographyUpdateRequest;
import bf.anptic.geoportail.dto.EquipmentFloorAssignmentRequest;
import bf.anptic.geoportail.dto.EquipmentResponse;
import bf.anptic.geoportail.dto.SiteAdminRequest;
import bf.anptic.geoportail.dto.SiteAdminResponse;
import bf.anptic.geoportail.service.backoffice.AdminSiteService;
import bf.anptic.geoportail.service.backoffice.EquipmentSyncService;
import bf.anptic.geoportail.service.backoffice.NetxmsSiteImportService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/backoffice/api/v1")
public class AdminSiteController {

    private final AdminSiteService adminSiteService;
    private final NetxmsSiteImportService netxmsSiteImportService;
    private final EquipmentSyncService equipmentSyncService;

    public AdminSiteController(AdminSiteService adminSiteService,
                                NetxmsSiteImportService netxmsSiteImportService,
                                EquipmentSyncService equipmentSyncService) {
        this.adminSiteService = adminSiteService;
        this.netxmsSiteImportService = netxmsSiteImportService;
        this.equipmentSyncService = equipmentSyncService;
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

    // Assigne/modifie l'etage d'un equipement deja decouvert automatiquement
    // depuis NetXMS. Ne cree ni ne supprime jamais d'equipement.
    @PutMapping("/equipments/{equipmentId}/etage")
    public EquipmentResponse assignEquipmentFloor(@PathVariable Long equipmentId,
                                                    @RequestBody EquipmentFloorAssignmentRequest request,
                                                    Authentication authentication) {
        return adminSiteService.assignFloor(equipmentId, request, authentication.getName(), authentication);
    }

    // Synchronise le catalogue des equipements LAN depuis netxmsdb
    // (geo_equipement, hors routeurs WAN). A appeler manuellement depuis
    // le backoffice, comme la synchronisation des sites.
    @PostMapping("/equipments/sync-netxms")
    public EquipmentSyncService.SyncResult syncEquipmentsFromNetxms() {
        return equipmentSyncService.syncEquipements();
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