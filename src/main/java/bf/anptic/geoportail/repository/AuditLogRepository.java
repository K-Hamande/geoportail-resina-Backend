package bf.anptic.geoportail.repository;

import bf.anptic.geoportail.model.AuditLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLogEntry, Long> {

    // Trie les entrees de la plus recente a la plus ancienne.
    // "OrderByHorodatageDesc" est, comme "findByActifTrue" plus tot,
    // une convention de nommage comprise automatiquement par Spring Data JPA.
    List<AuditLogEntry> findAllByOrderByHorodatageDesc();
}