package bf.anptic.geoportail.repository;

import bf.anptic.geoportail.model.NotificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationTokenRepository extends JpaRepository<NotificationToken, Long> {

    List<NotificationToken> findBySite_SiteIdAndActifTrue(String siteId);

    Optional<NotificationToken> findBySite_SiteIdAndEndpoint(String siteId, String endpoint);

    // Page Backoffice "Notifications push" : filtres optionnels (site,
    // ministere, ville, fenetre de dates sur enregistreLe) - chaque
    // parametre null desactive son filtre. LEFT JOIN (pas INNER) : les
    // abonnements "orphelins" (site_id NULL ou pointant vers un site
    // supprime) restent inclus tant qu'aucun filtre ministere/ville n'est
    // actif, comme avant ce correctif (filtres defensifs cote service,
    // cf. NotificationService.listTokens).
    //
    // BUG CORRIGE : dateDebut/dateFin etaient testes avec "(:x IS NULL OR
    // ...)", comme les filtres texte ci-dessus - mais a la difference de
    // ceux-ci, Postgres n'arrive PAS a deduire le type d'un parametre
    // timestamp qui n'apparait QUE dans un test "? is null" (meme bug,
    // meme diagnostic que documente dans IncidentHistoriqueRepository :
    // erreur "could not determine data type of parameter"). Plutot que de
    // tester IS NULL ici, dateDebut/dateFin sont desormais TOUJOURS
    // concrets (bornes par defaut resolues cote service, cf.
    // NotificationService.listTokens) et compares directement.
    @Query("""
            SELECT t FROM NotificationToken t LEFT JOIN t.site s
            WHERE (:siteId IS NULL OR s.siteId = :siteId)
              AND (:ministere IS NULL OR s.ministere = :ministere)
              AND (:ville IS NULL OR s.ville = :ville)
              AND t.enregistreLe >= :dateDebut
              AND t.enregistreLe < :dateFin
            ORDER BY t.enregistreLe DESC
            """)
    List<NotificationToken> rechercher(@Param("siteId") String siteId,
                                        @Param("ministere") String ministere,
                                        @Param("ville") String ville,
                                        @Param("dateDebut") Instant dateDebut,
                                        @Param("dateFin") Instant dateFin);
}
