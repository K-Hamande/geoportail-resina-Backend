package bf.anptic.geoportail.security;

import bf.anptic.geoportail.model.AdminUser;
import bf.anptic.geoportail.repository.AdminUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// Spring Security appelle automatiquement loadUserByUsername(...) quand
// quelqu'un tente de se connecter (login/mot de passe), pour verifier
// si l'utilisateur existe et recuperer son mot de passe hache.
@Service
public class BackofficeUserDetailsService implements UserDetailsService {

    private final AdminUserRepository adminUserRepository;

    public BackofficeUserDetailsService(AdminUserRepository adminUserRepository) {
        this.adminUserRepository = adminUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        AdminUser user = adminUserRepository.findByLoginAndActifTrue(login)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable : " + login));

        // User.builder() construit un objet UserDetails standard de
        // Spring Security a partir de nos propres donnees.
        return User.builder()
                .username(user.getLogin())
                .password(user.getMotDePasseHash())   // deja hache, Spring ne le hache pas une 2e fois
                .roles("ADMIN")                        // role generique pour l'instant
                .build();
    }
}