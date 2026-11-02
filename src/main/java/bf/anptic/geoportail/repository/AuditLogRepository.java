package bf.anptic.geoportail.repository;

import bf.anptic.geoportail.model.AuditLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLogEntry, Long> {

    // Trie les entrees de la plus recente a la plus ancienne.
    // "OrderByHorodatageDesc" est, comme "findByActifTrue" plus tot,
    // une convention de nommage comprise automatiquement par Spring Data JPA.
    List<AuditLogEntry> findAllByOrderByHorodatageDesc();

    // Retour utilisateur du 30/10/2026 : recherche/filtrage sur TOUT
    // l'historique (texte sur action/details, auteur exact), pas
    // seulement les 500 plus recents comme avant (cf. AdminAuditController,
    // ancien LIMITE_AFFICHAGE). Filtrage/pagination fait en memoire cote
    // service (AuditLogSearchService), meme principe que
    // IncidentHistoriqueRepository.rechercher - permet de calculer les
    // statistiques sur l'ensemble filtre avant de paginer.
    @Query("""
            SELECT a FROM AuditLogEntry a
            WHERE (:auteur IS NULL OR a.auteur = :auteur)
              AND (:recherche IS NULL OR LOWER(a.action) LIKE :recherche OR LOWER(a.details) LIKE :recherche)
            ORDER BY a.horodatage DESC
            """)
    List<AuditLogEntry> rechercher(@Param("auteur") String auteur, @Param("recherche") String recherche);
}
