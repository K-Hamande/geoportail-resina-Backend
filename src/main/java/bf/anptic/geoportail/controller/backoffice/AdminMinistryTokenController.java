package bf.anptic.geoportail.controller.backoffice;

import bf.anptic.geoportail.dto.MinistryAccessTokenCreateRequest;
import bf.anptic.geoportail.dto.MinistryAccessTokenDto;
import bf.anptic.geoportail.service.backoffice.AdminMinistryTokenService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/backoffice/api/v1/ministry-tokens")
public class AdminMinistryTokenController {

    private final AdminMinistryTokenService service;

    public AdminMinistryTokenController(AdminMinistryTokenService service) {
        this.service = service;
    }

    @GetMapping("/ministeres")
    public List<String> listMinisteres() {
        return service.listMinisteres();
    }

    @GetMapping
    public List<MinistryAccessTokenDto> listTokens() {
        return service.listTokens();
    }

    @PostMapping
    public MinistryAccessTokenDto createToken(@RequestBody MinistryAccessTokenCreateRequest request,
                                               Authentication authentication) {
        return service.createToken(request, authentication.getName());
    }

    @PostMapping("/{id}/deactivate")
    public void deactivate(@PathVariable Long id, Authentication authentication) {
        service.setActive(id, false, authentication.getName());
    }

    @PostMapping("/{id}/activate")
    public void activate(@PathVariable Long id, Authentication authentication) {
        service.setActive(id, true, authentication.getName());
    }
}