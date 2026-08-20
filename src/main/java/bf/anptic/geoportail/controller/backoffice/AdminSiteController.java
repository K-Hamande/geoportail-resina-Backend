package bf.anptic.geoportail.controller.backoffice;

import bf.anptic.geoportail.dto.CartographyItemDto;
import bf.anptic.geoportail.dto.CartographyUpdateRequest;
import bf.anptic.geoportail.dto.EquipmentResponse;
import bf.anptic.geoportail.dto.EquipmentStatsDto;
import bf.anptic.geoportail.dto.SiteAdminRequest;
import bf.anptic.geoportail.dto.SiteAdminResponse;
import bf.anptic.geoportail.model.Equipment;
import bf.anptic.geoportail.repository.EquipmentRepository;
import bf.anptic.geoportail.repository.SiteRepository;
import bf.anptic.geoportail.service.backoffice.AdminSiteService;
import bf.anptic.geoportail.service.backoffice.EquipmentSyncService;
import bf.anptic.geoportail.service.backoffice.NetxmsSiteImportService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/backoffice/api/v1")
public class AdminSiteController {

    private final AdminSiteService adminSiteService;
    private final NetxmsSiteImportService netxmsSiteImportService;
    private final EquipmentSyncService equipmentSyncService;
    private final EquipmentRepository equipmentRepository;
    private final SiteRepository siteRepository;

    public AdminSiteController(AdminSiteService adminSiteService,
                                NetxmsSiteImportService netxmsSiteImportService,
                                EquipmentSyncService equipmentSyncService,
                                EquipmentRepository equipmentRepository,
                                SiteRepository siteRepository) {
        this.adminSiteService = adminSiteService;
        this.netxmsSiteImportService = netxmsSiteImportService;
        this.equipmentSyncService = equipmentSyncService;
        this.equipmentRepository = equipmentRepository;
        this.siteRepository = siteRepository;
    }

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


    @PutMapping("/equipments/{equipmentId}/etage")
    public EquipmentResponse assignFloor(@PathVariable Long equipmentId,
                                          @RequestBody bf.anptic.geoportail.dto.EquipmentFloorAssignmentRequest request,
                                          Authentication authentication) {
        return adminSiteService.assignFloor(equipmentId, request, authentication.getName(), authentication);
    }

    @PostMapping("/equipments/sync-netxms")
    public EquipmentSyncService.SyncResult syncEquipments() {
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

    // ---- Equipements : compteur, stats, filtres ----

    @GetMapping("/equipments/count")
    public Map<String, Long> countEquipments() {
        return Map.of("total", equipmentRepository.count());
    }

    @GetMapping("/equipments/stats")
    public EquipmentStatsDto equipmentStats() {
        long total = equipmentRepository.count();

        Map<String, String> libelles = new LinkedHashMap<>();
        libelles.put("BORNE_WIFI", "Borne Wi-Fi");
        libelles.put("COMMUTATEUR", "Commutateur (Switch)");
        libelles.put("ROUTEUR", "Routeur");
        libelles.put("PTP", "Point-to-Point (PTP)");
        libelles.put("PMP", "Point-to-Multipoint (PMP)");
        libelles.put("CPE", "CPE (Terminaison opérateur)");
        libelles.put("ONDULEUR", "Onduleur (UPS)");
        libelles.put("SERVEUR", "Serveur");
        libelles.put("PYLONE", "Pylône / Tour");
        libelles.put("AUTRE", "Autre");

        List<EquipmentStatsDto.TypeStatDto> parType = equipmentRepository.countByType().stream()
                .map(row -> {
                    String type = row[0].toString();
                    long count = (Long) row[1];
                    double pct = total > 0 ? Math.round((count * 1000.0 / total)) / 10.0 : 0;
                    return new EquipmentStatsDto.TypeStatDto(type, libelles.getOrDefault(type, type), count, pct);
                })
                .toList();

        List<String> regions = siteRepository.findAll().stream()
                .map(s -> s.getRegionAdministrative())
                .filter(r -> r != null && !r.isBlank())
                .distinct().sorted().toList();

        List<String> villes = siteRepository.findAll().stream()
                .map(s -> s.getVille())
                .filter(v -> v != null && !v.isBlank())
                .distinct().sorted().toList();

        List<String> ministeres = siteRepository.findDistinctMinisteres();

        List<String> provinces = siteRepository.findDistinctProvinces(null);
        List<String> structures = siteRepository.findDistinctStructures(null);

        return new EquipmentStatsDto(total, parType, regions, provinces, villes, ministeres, structures);    
    }

        @GetMapping("/equipments")
    public List<EquipmentResponse> listEquipmentsFiltered(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String ville,
            @RequestParam(required = false) String ministere,
            @RequestParam(required = false) String structure,
            @RequestParam(required = false) String type) {

        Equipment.EquipmentType typeEnum = null;
        if (type != null && !type.isBlank()) {
            try { typeEnum = Equipment.EquipmentType.valueOf(type); }
            catch (IllegalArgumentException ignored) {}
        }

        return equipmentRepository.findWithFilters(
                region == null || region.isBlank() ? null : region,
                province == null || province.isBlank() ? null : province,
                ville == null || ville.isBlank() ? null : ville,
                ministere == null || ministere.isBlank() ? null : ministere,
                structure == null || structure.isBlank() ? null : structure,
                typeEnum
        ).stream()
         .map(eq -> new EquipmentResponse(
                 eq.getId(),
                 eq.getSite() != null ? eq.getSite().getSiteId() : null,
                 eq.getEtageLabel(),
                 eq.getType() != null ? eq.getType().name() : null,
                 eq.getLibelleAffiche(),
                 eq.getNomTechniqueNetxms(),
                 eq.getNetxmsObjectId()
         ))
         .toList();
    }
        // Endpoint pour les filtres en cascade (provinces d'une region,
    // villes d'une province, structures d'un ministere)
    @GetMapping("/equipments/cascade")
    public List<String> cascade(
            @RequestParam String niveau,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String ministere) {
        return switch (niveau) {
            case "provinces" -> siteRepository.findDistinctProvinces(region);
            case "villes" -> siteRepository.findDistinctVilles(province);
            case "structures" -> siteRepository.findDistinctStructures(ministere);
            default -> List.of();
        };
    }
}