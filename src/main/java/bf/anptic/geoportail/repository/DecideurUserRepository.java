package bf.anptic.geoportail.repository;

import bf.anptic.geoportail.model.DecideurUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DecideurUserRepository extends JpaRepository<DecideurUser, Long> {
    Optional<DecideurUser> findByLoginAndActifTrue(String login);
    List<DecideurUser> findAllByOrderByCreeLeDesc();
}