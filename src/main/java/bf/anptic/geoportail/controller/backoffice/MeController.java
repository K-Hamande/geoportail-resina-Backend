package bf.anptic.geoportail.controller.backoffice;

import bf.anptic.geoportail.dto.ChangeOwnPasswordRequest;
import bf.anptic.geoportail.dto.CurrentUserResponse;
import bf.anptic.geoportail.service.backoffice.AdminUserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// Self-service : n'importe quel utilisateur Backoffice connecte (SUPER_ADMIN,
// ADMIN_DEST ou ADMIN_DIG) peut consulter son propre profil et changer son
// propre mot de passe - contrairement a AdminUserController (reserve a
// SUPER_ADMIN), pas de @PreAuthorize par role ici : la seule exigence est
// d'etre authentifie, deja garantie par SecurityConfig pour
// /backoffice/api/v1/**.
@RestController
@RequestMapping("/backoffice/api/v1/me")
public class MeController {

    private final AdminUserService adminUserService;

    public MeController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public CurrentUserResponse getMe(Authentication authentication) {
        return adminUserService.getCurrentUser(authentication.getName());
    }

    @PostMapping("/password")
    public void changePassword(@RequestBody ChangeOwnPasswordRequest request, Authentication authentication) {
        adminUserService.changeOwnPassword(authentication.getName(), request.motDePasseActuel(), request.nouveauMotDePasse());
    }
}