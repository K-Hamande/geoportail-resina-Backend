package bf.anptic.geoportail.controller.backoffice;

import bf.anptic.geoportail.dto.SupervisionSettingsDto;
import bf.anptic.geoportail.dto.SupervisionSettingsRequest;
import bf.anptic.geoportail.service.backoffice.AdminSupervisionService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/backoffice/api/v1/supervision")
public class AdminSupervisionController {

    private final AdminSupervisionService service;

    public AdminSupervisionController(AdminSupervisionService service) {
        this.service = service;
    }

    @GetMapping
    public List<SupervisionSettingsDto> listSettings() {
        return service.listSettings();
    }

    @PutMapping("/{siteId}")
    public SupervisionSettingsDto updateSettings(@PathVariable String siteId,
                                                  @RequestBody SupervisionSettingsRequest request,
                                                  Authentication authentication) {
        return service.updateSettings(siteId, request, authentication.getName());
    }

    @DeleteMapping("/{siteId}")
    public void resetToDefaults(@PathVariable String siteId, Authentication authentication) {
        service.resetToDefaults(siteId, authentication.getName());
    }
}