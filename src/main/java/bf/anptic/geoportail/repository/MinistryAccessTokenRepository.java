package bf.anptic.geoportail.repository;

import bf.anptic.geoportail.model.MinistryAccessToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MinistryAccessTokenRepository extends JpaRepository<MinistryAccessToken, Long> {
    Optional<MinistryAccessToken> findByTokenAndActifTrue(String token);
    List<MinistryAccessToken> findAllByOrderByCreeLeDesc();
}