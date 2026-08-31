package bf.anptic.geoportail.service.backoffice;

import bf.anptic.geoportail.dto.AdminUserCreateRequest;
import bf.anptic.geoportail.dto.AdminUserResponse;
import bf.anptic.geoportail.dto.CurrentUserResponse;
import bf.anptic.geoportail.model.AdminUser;
import bf.anptic.geoportail.repository.AdminUserRepository;
import bf.anptic.geoportail.service.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import bf.anptic.geoportail.dto.AdminUserUpdateRequest;

import java.util.List;

@Service
public class AdminUserService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    // PasswordEncoder est deja declare comme @Bean dans SecurityConfig
    // (Etape 9.7) - Spring nous l'injecte ici automatiquement, exactement
    // comme un Repository.
    public AdminUserService(AdminUserRepository adminUserRepository,
                             PasswordEncoder passwordEncoder,
                             AuditService auditService) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

public AdminUserResponse createUser(AdminUserCreateRequest request, String auteur) {
        AdminUser user = new AdminUser();
        user.setLogin(request.login());
        user.setNomComplet(request.nomComplet());
        user.setMotDePasseHash(passwordEncoder.encode(request.motDePasse()));
        user.setRole(AdminUser.Role.valueOf(request.role()));
        user.setSitesAutorises(request.sitesAutorises() != null ? request.sitesAutorises() : java.util.Set.of());
        user.setActif(true);

        AdminUser saved = adminUserRepository.save(user);
        auditService.record(auteur, "Création utilisateur Backoffice",
                "login=" + saved.getLogin() + " role=" + saved.getRole());

        return toResponse(saved);
    }
public AdminUserResponse updateUser(Long userId, AdminUserUpdateRequest request, String auteur) {
        AdminUser user = findUserOrThrow(userId);

        user.setNomComplet(request.nomComplet());
        user.setRole(AdminUser.Role.valueOf(request.role()));
        user.setSitesAutorises(request.sitesAutorises() != null ? request.sitesAutorises() : java.util.Set.of());

        AdminUser saved = adminUserRepository.save(user);
        auditService.record(auteur, "Modification utilisateur Backoffice",
                "login=" + saved.getLogin() + " nom=" + saved.getNomComplet());

        return toResponse(saved);
    }
    public List<AdminUserResponse> listUsers() {
        return adminUserRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public void resetPassword(Long userId, String nouveauMotDePasse, String auteur) {
        AdminUser user = findUserOrThrow(userId);
        user.setMotDePasseHash(passwordEncoder.encode(nouveauMotDePasse));
        adminUserRepository.save(user);
        auditService.record(auteur, "Réinitialisation mot de passe", "login=" + user.getLogin());
    }

    public void setActive(Long userId, boolean active, String auteur) {
        AdminUser user = findUserOrThrow(userId);
        user.setActif(active);
        adminUserRepository.save(user);
        auditService.record(auteur,
                active ? "Réactivation utilisateur" : "Désactivation utilisateur",
                "login=" + user.getLogin());
    }

    // Suppression definitive d'un compte Backoffice. On interdit qu'un
    // admin se supprime lui-meme (comparaison sur le login, insensible a
    // la casse) pour eviter de se retrouver bloque hors de l'appli.
    public void deleteUser(Long userId, String auteur) {
        AdminUser user = findUserOrThrow(userId);
        if (user.getLogin() != null && user.getLogin().equalsIgnoreCase(auteur)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Vous ne pouvez pas supprimer votre propre compte.");
        }
        adminUserRepository.delete(user);
        auditService.record(auteur, "Suppression utilisateur Backoffice", "login=" + user.getLogin());
    }

    // ---- Self-service "Mon profil" (accessible a tout role, pas seulement
    // SUPER_ADMIN - voir MeController) ----

    public CurrentUserResponse getCurrentUser(String login) {
        AdminUser user = adminUserRepository.findByLoginAndActifTrue(login)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable."));
        return new CurrentUserResponse(user.getId(), user.getLogin(), user.getNomComplet(), user.getRole().name());
    }

    public void changeOwnPassword(String login, String motDePasseActuel, String nouveauMotDePasse) {
        AdminUser user = adminUserRepository.findByLoginAndActifTrue(login)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable."));

        if (motDePasseActuel == null || !passwordEncoder.matches(motDePasseActuel, user.getMotDePasseHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mot de passe actuel incorrect.");
        }
        if (nouveauMotDePasse == null || nouveauMotDePasse.isBlank() || nouveauMotDePasse.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le nouveau mot de passe doit contenir au moins 6 caracteres.");
        }

        user.setMotDePasseHash(passwordEncoder.encode(nouveauMotDePasse));
        adminUserRepository.save(user);
        auditService.record(login, "Changement de mot de passe (self-service)", "login=" + user.getLogin());
    }

    private AdminUser findUserOrThrow(Long userId) {
        return adminUserRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Utilisateur introuvable : " + userId));
    }

    // Methode privee de "mapping" : convertit l'entite JPA en DTO de
    // reponse, en excluant volontairement motDePasseHash.
    private AdminUserResponse toResponse(AdminUser user) {
        return new AdminUserResponse(
                user.getId(), user.getLogin(), user.getNomComplet(),
                user.getRole().name(), user.getSitesAutorises(), user.getActif()
        );
    }
}