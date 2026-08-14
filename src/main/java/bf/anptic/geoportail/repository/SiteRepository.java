package bf.anptic.geoportail.repository;

import bf.anptic.geoportail.model.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SiteRepository extends JpaRepository<Site, String> {
    List<Site> findByActifTrue();
    List<Site> findByActifTrueAndMinistere(String ministere);

    // Requete JPQL explicite plutot que derivee du nom de methode :
    // la combinaison Distinct + OrderBy sur une projection a colonne
    // unique fait planter Hibernate 6 ("multiple selections") quand
    // elle est generee automatiquement a partir du nom de methode.
    @Query("SELECT DISTINCT s.ministere FROM Site s WHERE s.actif = true AND s.ministere IS NOT NULL ORDER BY s.ministere")
    List<String> findDistinctMinisteres();
}