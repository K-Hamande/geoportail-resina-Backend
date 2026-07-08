package bf.anptic.geoportail.security;

import bf.anptic.geoportail.model.AdminUser;
import bf.anptic.geoportail.repository.AdminUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

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

        // .roles(...) genere automatiquement des autorites "ROLE_XXX"
        // (ex: ROLE_SUPER_ADMIN) - c'est la convention attendue par
        // hasRole("SUPER_ADMIN") qu'on utilisera dans les controleurs.
        return User.builder()
                .username(user.getLogin())
                .password(user.getMotDePasseHash())
                .roles(user.getRole().name())
                .build();
    }
}