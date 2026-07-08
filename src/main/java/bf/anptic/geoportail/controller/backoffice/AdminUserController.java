package bf.anptic.geoportail.controller.backoffice;

import bf.anptic.geoportail.dto.AdminUserCreateRequest;
import bf.anptic.geoportail.dto.AdminUserResponse;
import bf.anptic.geoportail.dto.ResetPasswordRequest;
import bf.anptic.geoportail.service.backoffice.AdminUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/backoffice/api/v1/users")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public List<AdminUserResponse> listUsers() {
        return adminUserService.listUsers();
    }

    @PostMapping
    public AdminUserResponse createUser(@RequestBody AdminUserCreateRequest request, Authentication authentication) {
        return adminUserService.createUser(request, authentication.getName());
    }

    @PostMapping("/{userId}/reset-password")
    public void resetPassword(@PathVariable Long userId,
                               @RequestBody ResetPasswordRequest request,
                               Authentication authentication) {
        adminUserService.resetPassword(userId, request.nouveauMotDePasse(), authentication.getName());
    }

    @PostMapping("/{userId}/deactivate")
    public void deactivate(@PathVariable Long userId, Authentication authentication) {
        adminUserService.setActive(userId, false, authentication.getName());
    }

    @PostMapping("/{userId}/activate")
    public void activate(@PathVariable Long userId, Authentication authentication) {
        adminUserService.setActive(userId, true, authentication.getName());
    }
}