package bf.anptic.geoportail.controller.backoffice;

import bf.anptic.geoportail.dto.DecideurAuthDto;
import bf.anptic.geoportail.service.DecideurAuthService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/backoffice/api/v1/decideur-users")
public class AdminDecideurController {

    private final DecideurAuthService decideurAuthService;

    public AdminDecideurController(DecideurAuthService decideurAuthService) {
        this.decideurAuthService = decideurAuthService;
    }

    @GetMapping
    public List<DecideurAuthDto.DecideurUserResponse> listUsers() {
        return decideurAuthService.listUsers();
    }

    @PostMapping
    public DecideurAuthDto.DecideurUserResponse createUser(
            @RequestBody DecideurAuthDto.DecideurUserRequest request,
            Authentication authentication) {
        return decideurAuthService.createUser(request, authentication.getName());
    }

    @PutMapping("/{id}")
    public DecideurAuthDto.DecideurUserResponse updateUser(
            @PathVariable Long id,
            @RequestBody DecideurAuthDto.DecideurUserRequest request,
            Authentication authentication) {
        return decideurAuthService.updateUser(id, request, authentication.getName());
    }

    @PostMapping("/{id}/activate")
    public void activate(@PathVariable Long id, Authentication authentication) {
        decideurAuthService.setActive(id, true, authentication.getName());
    }

    @PostMapping("/{id}/deactivate")
    public void deactivate(@PathVariable Long id, Authentication authentication) {
        decideurAuthService.setActive(id, false, authentication.getName());
    }
}