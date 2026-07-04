package bf.anptic.geoportail.repository;

import bf.anptic.geoportail.model.Site;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SiteRepository extends JpaRepository<Site, String> {

    // Utilisee cote DECIDEUR : ne renvoie que les sites actifs (§3.2.6b)
    List<Site> findByActifTrue();
}