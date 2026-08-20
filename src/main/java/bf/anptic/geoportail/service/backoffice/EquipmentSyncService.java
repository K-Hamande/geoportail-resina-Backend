package bf.anptic.geoportail.service.backoffice;

import bf.anptic.geoportail.model.Equipment;
import bf.anptic.geoportail.model.Equipment.EquipmentType;
import bf.anptic.geoportail.model.Site;
import bf.anptic.geoportail.repository.EquipmentRepository;
import bf.anptic.geoportail.repository.SiteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Synchronise le catalogue COMPLET des equipements depuis netxmsdb
// (geo_equipement) vers la table applicative equipments. Contrairement
// a la version precedente qui excluait les routeurs (reserves pour le
// WAN via /anptic), on importe desormais TOUS les types afin d'alimenter
// les statistiques par type d'equipement dans le Backoffice.
// Les champs etageLabel et libelleAffiche ne sont JAMAIS ecrases par
// une resynchronisation - ils sont proteges pour les assignations
// manuelles.
@Service
public class EquipmentSyncService {

    private static final Logger log = LoggerFactory.getLogger(EquipmentSyncService.class);

    // Plus de filtre "hors routeurs" : on importe TOUT.
    private static final String SELECT_EQUIPEMENTS = """
            SELECT object_id, siteadmin_id, name, type
            FROM public.geo_equipement
            ORDER BY siteadmin_id
            """;

    private final JdbcTemplate netxmsJdbcTemplate;
    private final EquipmentRepository equipmentRepository;
    private final SiteRepository siteRepository;

    public EquipmentSyncService(@Qualifier("netxmsJdbcTemplate") JdbcTemplate netxmsJdbcTemplate,
                                 EquipmentRepository equipmentRepository,
                                 SiteRepository siteRepository) {
        this.netxmsJdbcTemplate = netxmsJdbcTemplate;
        this.equipmentRepository = equipmentRepository;
        this.siteRepository = siteRepository;
    }

    public static class SyncResult {
        public int crees;
        public int misAJour;
        public int ignores;
        public int total;
    }

    @Transactional
    public SyncResult syncEquipements() {
        Map<Integer, Site> siteByNetxmsNodeId = new HashMap<>();
        for (Site site : siteRepository.findAll()) {
            if (site.getNetxmsNodeId() != null) {
                siteByNetxmsNodeId.put(site.getNetxmsNodeId(), site);
            }
        }

        List<EquipementRow> rows = netxmsJdbcTemplate.query(SELECT_EQUIPEMENTS, (rs, rowNum) ->
                new EquipementRow(
                        rs.getInt("object_id"),
                        rs.getInt("siteadmin_id"),
                        rs.getString("name"),
                        rs.getString("type")
                ));

        SyncResult result = new SyncResult();
        result.total = rows.size();

        for (EquipementRow row : rows) {
            Site site = siteByNetxmsNodeId.get(row.siteadminId());
            if (site == null) {
                result.ignores++;
                continue;
            }

            Optional<Equipment> existant = equipmentRepository.findByNetxmsObjectId(row.objectId());

            if (existant.isPresent()) {
                Equipment eq = existant.get();
                eq.setSite(site);
                eq.setType(mapType(row.type()));
                eq.setNomTechniqueNetxms(row.name());
                equipmentRepository.save(eq);
                result.misAJour++;
            } else {
                Equipment eq = new Equipment();
                eq.setSite(site);
                eq.setNetxmsObjectId(row.objectId());
                eq.setType(mapType(row.type()));
                eq.setNomTechniqueNetxms(row.name());
                eq.setEtageLabel(null);
                eq.setLibelleAffiche(null);
                equipmentRepository.save(eq);
                result.crees++;
            }
        }

        log.info("Sync equipements termine : {} crees, {} mis a jour, {} ignores, {} au total",
                result.crees, result.misAJour, result.ignores, result.total);

        return result;
    }

    // Mapping des types NetXMS vers notre enum applicatif. On utilise ILIKE
    // (comparaison en minuscules, avec correspondance partielle) plutot que
    // des egalites strictes, pour tolerer les variantes de nommage entre
    // installations NetXMS ("Router", "router", "Routeur"...).
    private static EquipmentType mapType(String netxmsType) {
        if (netxmsType == null) return EquipmentType.AUTRE;
        String t = netxmsType.trim().toLowerCase();

        if (t.contains("switch") || t.contains("commut")) return EquipmentType.COMMUTATEUR;
        if (t.contains("rout")) return EquipmentType.ROUTEUR;
        if (t.contains("ptp") || t.contains("point-to-point") || t.contains("point to point")) return EquipmentType.PTP;
        if (t.contains("pmp") || t.contains("multipoint")) return EquipmentType.PMP;
        if (t.contains("cpe")) return EquipmentType.CPE;
        if (t.contains("onduleur") || t.contains("ups")) return EquipmentType.ONDULEUR;
        if (t.contains("serveur") || t.contains("server")) return EquipmentType.SERVEUR;
        if (t.contains("pylone") || t.contains("pylône") || t.contains("tower") || t.contains("pylon")) return EquipmentType.PYLONE;
        if (t.contains("wifi") || t.contains("wi-fi") || t.contains("borne") || t.contains("ap") || t.contains("access")) return EquipmentType.BORNE_WIFI;

        return EquipmentType.AUTRE;
    }

    private record EquipementRow(int objectId, int siteadminId, String name, String type) {}
}