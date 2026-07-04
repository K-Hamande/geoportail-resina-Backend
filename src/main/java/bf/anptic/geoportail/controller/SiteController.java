package bf.anptic.geoportail.controller;

import bf.anptic.geoportail.dto.AnpticStatusDto;
import bf.anptic.geoportail.dto.LanStatusDto;
import bf.anptic.geoportail.dto.MapSiteDto;
import bf.anptic.geoportail.dto.RegisterTokenRequest;
import bf.anptic.geoportail.dto.SiteSummaryDto;
import bf.anptic.geoportail.repository.SiteRepository;
import bf.anptic.geoportail.service.AnpticStatusService;
import bf.anptic.geoportail.service.LanStatusService;
import bf.anptic.geoportail.service.MapService;
import bf.anptic.geoportail.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class SiteController {

    private final SiteRepository siteRepository;
    private final AnpticStatusService anpticStatusService;
    private final LanStatusService lanStatusService;
    private final NotificationService notificationService;
    private final MapService mapService;

    public SiteController(SiteRepository siteRepository,
                           AnpticStatusService anpticStatusService,
                           LanStatusService lanStatusService,
                           NotificationService notificationService,
                           MapService mapService) {
        this.siteRepository = siteRepository;
        this.anpticStatusService = anpticStatusService;
        this.lanStatusService = lanStatusService;
        this.notificationService = notificationService;
        this.mapService = mapService;
    }

    @GetMapping("/sites")
    public List<SiteSummaryDto> listSites() {
        return siteRepository.findByActifTrue().stream()
                .map(site -> new SiteSummaryDto(site.getSiteId(), site.getNom(), site.getVille()))
                .toList();
    }

    @GetMapping("/site/{id}/anptic")
    public AnpticStatusDto getAnpticStatus(@PathVariable("id") String siteId) {
        return anpticStatusService.getAnpticStatus(siteId);
    }

    @GetMapping("/site/{id}/lan")
    public LanStatusDto getLanStatus(@PathVariable("id") String siteId) {
        return lanStatusService.getLanStatus(siteId);
    }

    @PostMapping("/site/{id}/notifications/register")
    public void registerNotificationToken(@PathVariable("id") String siteId,
                                           @RequestBody RegisterTokenRequest request) {
        notificationService.registerToken(siteId, request);
    }

    // NOUVEAU : vue "Carte" (Amendement 1, Annexe A.2)
    @GetMapping("/sites/map")
    public List<MapSiteDto> listSitesForMap() {
        return mapService.listSitesForMap();
    }
}