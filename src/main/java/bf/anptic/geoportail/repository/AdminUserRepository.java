package bf.anptic.geoportail.repository;

import bf.anptic.geoportail.model.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

    Optional<AdminUser> findByLoginAndActifTrue(String login);

    boolean existsByLogin(String login);
}