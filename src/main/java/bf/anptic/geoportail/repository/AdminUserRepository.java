package bf.anptic.geoportail.repository;

import bf.anptic.geoportail.model.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

    // "query method" : Spring Data JPA genere automatiquement
    // "SELECT * FROM admin_users WHERE login = ? AND actif = true"
    Optional<AdminUser> findByLoginAndActifTrue(String login);
}