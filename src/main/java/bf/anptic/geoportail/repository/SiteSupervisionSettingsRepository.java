package bf.anptic.geoportail.repository;

import bf.anptic.geoportail.model.SiteSupervisionSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteSupervisionSettingsRepository extends JpaRepository<SiteSupervisionSettings, String> {
}