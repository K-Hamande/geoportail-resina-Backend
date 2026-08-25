package bf.anptic.geoportail.repository;

import bf.anptic.geoportail.model.Equipment;
import bf.anptic.geoportail.model.Equipment.EquipmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    List<Equipment> findBySite_SiteId(String siteId);
    long countBySite_SiteId(String siteId);
    Optional<Equipment> findByNetxmsObjectId(Integer netxmsObjectId);

    // Statistiques : nombre d'equipements par type (pour le graphe du backoffice)
    @Query("SELECT e.type, COUNT(e) FROM Equipment e GROUP BY e.type ORDER BY COUNT(e) DESC")
    List<Object[]> countByType();

    // Filtres : liste des equipements avec filtrage optionnel par region/ville/ministere.
    // LEFT JOIN (et non plus INNER JOIN) : un equipement dont le site n'a pas
    // (ou plus) de correspondance dans la table sites doit quand meme
    // apparaitre dans l'inventaire general - seuls les filtres geographiques
    // actifs doivent l'exclure, pas l'absence de site en elle-meme.
    @Query("""
            SELECT e FROM Equipment e
            LEFT JOIN e.site s
            WHERE (:region IS NULL OR s.regionAdministrative = :region)
              AND (:province IS NULL OR s.province = :province)
              AND (:ville IS NULL OR s.ville = :ville OR s.ville = CONCAT(:ville, '\\r\\n'))
              AND (:ministere IS NULL OR s.ministere = :ministere)
              AND (:structure IS NULL OR s.structure = :structure)
              AND (:type IS NULL OR e.type = :type)
            ORDER BY s.nom, e.type
            """)
    List<Equipment> findWithFilters(
            @Param("region") String region,
            @Param("province") String province,
            @Param("ville") String ville,
            @Param("ministere") String ministere,
            @Param("structure") String structure,
            @Param("type") EquipmentType type
    );

    // Repartition par type, avec les memes filtres geographiques que
    // findWithFilters (mais sans filtre sur le type lui-meme : chaque
    // carte de repartition doit rester visible pour permettre de basculer
    // d'un type a l'autre). Utilisee pour recalculer "Repartition par type
    // d'equipement" a chaque changement de filtre region/province/ville/
    // ministere/structure cote Backoffice.
    @Query("""
            SELECT e.type, COUNT(e) FROM Equipment e
            LEFT JOIN e.site s
            WHERE (:region IS NULL OR s.regionAdministrative = :region)
              AND (:province IS NULL OR s.province = :province)
              AND (:ville IS NULL OR s.ville = :ville OR s.ville = CONCAT(:ville, '\\r\\n'))
              AND (:ministere IS NULL OR s.ministere = :ministere)
              AND (:structure IS NULL OR s.structure = :structure)
            GROUP BY e.type
            ORDER BY COUNT(e) DESC
            """)
    List<Object[]> countByTypeWithFilters(
            @Param("region") String region,
            @Param("province") String province,
            @Param("ville") String ville,
            @Param("ministere") String ministere,
            @Param("structure") String structure
    );
}