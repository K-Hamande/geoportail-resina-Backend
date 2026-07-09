package bf.anptic.geoportail.repository;

import bf.anptic.geoportail.model.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    // Spring Data JPA comprend le nom de la methode et genere
    // automatiquement la requete correspondante : ici,
    // "SELECT * FROM equipments WHERE site_id = ?"
    // C'est ce qu'on appelle une "query method" : pas une ligne de SQL
    // ecrite a la main, juste un nom de methode respectant une convention.
    List<Equipment> findBySite_SiteId(String siteId);

    // Meme principe, version comptage :
    // "SELECT COUNT(*) FROM equipments WHERE site_id = ?"
    // Utilisee par AdminSiteService.toSiteResponse pour afficher
    // le nombre d'equipements de chaque site.
    long countBySite_SiteId(String siteId);
}