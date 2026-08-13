package bf.anptic.geoportail.service;

import bf.anptic.geoportail.dto.MapSiteDto;
import org.springframework.stereotype.Service;

import java.util.List;

// Combine le statut ANPTIC et le statut LAN de chaque site actif pour
// produire le statut global affiche sur la carte (Amendement 1, A.2).
// S'appuie sur SiteStatusSnapshotService, qui calcule tout en 4 requetes
// SQL au total (voir ce service pour le detail) - plutot que d'interroger
// netxmsdb site par site, ce qui serait beaucoup trop lent sur 1600+ sites.
@Service
public class MapService {

    private final SiteStatusSnapshotService snapshotService;

    public MapService(SiteStatusSnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    public List<MapSiteDto> listSitesForMap() {
        return snapshotService.computeAll().stream()
                .map(s -> new MapSiteDto(
                        s.site().getSiteId(),
                        s.site().getNom(),
                        s.site().getVille(),
                        s.site().getLatitude(),
                        s.site().getLongitude(),
                        s.statutGlobal()
                ))
                .toList();
    }
}