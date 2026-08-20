package bf.anptic.geoportail.repository;

import bf.anptic.geoportail.model.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SiteRepository extends JpaRepository<Site, String> {
    List<Site> findByActifTrue();
    List<Site> findByActifTrueAndMinistere(String ministere);

    @Query("SELECT DISTINCT s.ministere FROM Site s WHERE s.actif = true AND s.ministere IS NOT NULL ORDER BY s.ministere")
    List<String> findDistinctMinisteres();

    // Filtres en cascade : provinces d'une region
    @Query("SELECT DISTINCT s.province FROM Site s WHERE s.actif = true AND s.province IS NOT NULL AND (:region IS NULL OR s.regionAdministrative = :region) ORDER BY s.province")
    List<String> findDistinctProvinces(@Param("region") String region);

    // Villes d'une province (ou toutes si province null)
    @Query("SELECT DISTINCT s.ville FROM Site s WHERE s.actif = true AND s.ville IS NOT NULL AND (:province IS NULL OR s.province = :province) ORDER BY s.ville")
    List<String> findDistinctVilles(@Param("province") String province);

    // Structures d'un ministere (ou toutes si ministere null)
    @Query("SELECT DISTINCT s.structure FROM Site s WHERE s.actif = true AND s.structure IS NOT NULL AND (:ministere IS NULL OR s.ministere = :ministere) ORDER BY s.structure")
    List<String> findDistinctStructures(@Param("ministere") String ministere);

    // Regions distinctes
    @Query("SELECT DISTINCT s.regionAdministrative FROM Site s WHERE s.actif = true AND s.regionAdministrative IS NOT NULL ORDER BY s.regionAdministrative")
    List<String> findDistinctRegions();
}