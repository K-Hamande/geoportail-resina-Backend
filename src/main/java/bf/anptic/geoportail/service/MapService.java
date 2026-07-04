package bf.anptic.geoportail.service;

import bf.anptic.geoportail.dto.MapSiteDto;
import bf.anptic.geoportail.model.Site;
import bf.anptic.geoportail.model.enums.NodeStatus;
import bf.anptic.geoportail.repository.SiteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

// Combine le statut ANPTIC et le statut LAN de chaque site actif pour
// produire le statut global affiche sur la carte (Amendement 1, A.2).
@Service
public class MapService {

    private final SiteRepository siteRepository;
    private final AnpticStatusService anpticStatusService;
    private final LanStatusService lanStatusService;

    public MapService(SiteRepository siteRepository,
                       AnpticStatusService anpticStatusService,
                       LanStatusService lanStatusService) {
        this.siteRepository = siteRepository;
        this.anpticStatusService = anpticStatusService;
        this.lanStatusService = lanStatusService;
    }

    public List<MapSiteDto> listSitesForMap() {
        return siteRepository.findByActifTrue().stream()
                .map(this::toMapSiteDto)
                .toList();
    }

    private MapSiteDto toMapSiteDto(Site site) {
        NodeStatus anpticStatus = anpticStatusService.getAnpticStatus(site.getSiteId()).status();
        NodeStatus lanStatus = lanStatusService.getLanStatus(site.getSiteId()).globalStatus();
        NodeStatus statutGlobal = NodeStatus.worstOf(anpticStatus, lanStatus);

        return new MapSiteDto(
                site.getSiteId(),
                site.getNom(),
                site.getVille(),
                site.getLatitude(),
                site.getLongitude(),
                statutGlobal
        );
    }
}