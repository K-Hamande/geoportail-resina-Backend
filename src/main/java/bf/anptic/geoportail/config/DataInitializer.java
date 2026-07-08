package bf.anptic.geoportail.config;

import bf.anptic.geoportail.model.AdminUser;
import bf.anptic.geoportail.repository.AdminUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapProperties bootstrapProperties;

    public DataInitializer(AdminUserRepository adminUserRepository,
                            PasswordEncoder passwordEncoder,
                            BootstrapProperties bootstrapProperties) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapProperties = bootstrapProperties;
    }

    @Override
    public void run(String... args) {
        String login = bootstrapProperties.getAdminLogin();

        if (adminUserRepository.existsByLogin(login)) {
            log.info("Compte super-admin '{}' deja present, aucune action.", login);
            return;
        }

        AdminUser superAdmin = new AdminUser();
        superAdmin.setLogin(login);
        superAdmin.setNomComplet(bootstrapProperties.getAdminNomComplet());
        superAdmin.setMotDePasseHash(passwordEncoder.encode(bootstrapProperties.getAdminPassword()));
        superAdmin.setRole(AdminUser.Role.SUPER_ADMIN);
        superAdmin.setActif(true);

        adminUserRepository.save(superAdmin);

        log.warn("=====================================================================");
        log.warn(" Compte super-admin Backoffice cree automatiquement : login = '{}'", login);
        log.warn(" Pensez a changer le mot de passe initial des la premiere connexion.");
        log.warn("=====================================================================");
    }
}