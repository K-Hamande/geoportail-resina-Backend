package bf.anptic.geoportail.controller;

import bf.anptic.geoportail.dto.CommuneCouvertureDto;
import bf.anptic.geoportail.dto.CouvertureStatsDto;
import bf.anptic.geoportail.service.CouvertureNationaleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

// Page publique "Geoportail national" (couverture RESINA par commune) -
// entierement publique, sans authentification (meme esprit que la liste
// de sites "utilisateur lambda" : consultable sans compte). Route
// exemptee du JWT decideur dans SiteAccessTokenFilter.
@RestController
@RequestMapping("/api/v1/couverture")
public class CouvertureNationaleController {

    private final CouvertureNationaleService couvertureNationaleService;

    public CouvertureNationaleController(CouvertureNationaleService couvertureNationaleService) {
        this.couvertureNationaleService = couvertureNationaleService;
    }

    @GetMapping("/stats")
    public CouvertureStatsDto getStats() {
        return couvertureNationaleService.getStats();
    }

    @GetMapping("/communes")
    public List<CommuneCouvertureDto> listCommunes() {
        return couvertureNationaleService.listCommunes();
    }

    @GetMapping("/communes/geojson")
    public Map<String, Object> getCommunesGeoJson() {
        return couvertureNationaleService.getCommunesGeoJson();
    }

    @GetMapping("/sites/geojson")
    public Map<String, Object> getSitesGeoJson() {
        return couvertureNationaleService.getSitesGeoJson();
    }

    @GetMapping("/liaisons/geojson")
    public Map<String, Object> getLiaisonsGeoJson() {
        return couvertureNationaleService.getLiaisonsGeoJson();
    }
}
