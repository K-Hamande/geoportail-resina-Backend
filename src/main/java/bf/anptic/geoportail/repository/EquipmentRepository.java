package bf.anptic.geoportail.repository;

import bf.anptic.geoportail.model.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    List<Equipment> findBySite_SiteId(String siteId);

    long countBySite_SiteId(String siteId);

    // Utilise par EquipmentSyncService pour retrouver un equipement deja
    // synchronise (upsert) a partir de son identifiant NetXMS, qui est
    // unique sur l'ensemble du reseau (pas seulement par site).
    Optional<Equipment> findByNetxmsObjectId(Integer netxmsObjectId);
}