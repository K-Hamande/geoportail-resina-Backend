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

// Synchronise le catalogue des equipements LAN (Wi-Fi/commutateurs et
// assimiles) depuis netxmsdb (geo_equipement) vers la table applicative
// equipments. Le routeur WAN de chaque site est exclu (deja affiche par
// /anptic). Les equipements ne sont JAMAIS crees/supprimes a la main dans
// le backoffice - seuls leur etage (etageLabel) et leur libelle affiche
// (libelleAffiche) sont modifiables manuellement, et jamais ecrases par
// une resynchronisation.
@Service
public class EquipmentSyncService {

    private static final Logger log = LoggerFactory.getLogger(EquipmentSyncService.class);

    private static final String SELECT_EQUIPEMENTS_LAN = """
            SELECT object_id, siteadmin_id, name, type
            FROM public.geo_equipement
            WHERE type NOT ILIKE '%rout%'
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

        List<EquipementRow> rows = netxmsJdbcTemplate.query(SELECT_EQUIPEMENTS_LAN, (rs, rowNum) ->
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
                // etageLabel et libelleAffiche volontairement PAS touches :
                // assignations manuelles preservees d'un sync a l'autre.
                equipmentRepository.save(eq);
                result.misAJour++;
            } else {
                Equipment eq = new Equipment();
                eq.setSite(site);
                eq.setNetxmsObjectId(row.objectId());
                eq.setType(mapType(row.type()));
                eq.setNomTechniqueNetxms(row.name());
                eq.setEtageLabel(null);       // a assigner manuellement
                eq.setLibelleAffiche(null);   // reste null -> libelle generique a l'affichage (voir LanStatusService)
                equipmentRepository.save(eq);
                result.crees++;
            }
        }

        log.info("Sync equipements termine : {} crees, {} mis a jour, {} ignores (site inconnu), {} au total",
                result.crees, result.misAJour, result.ignores, result.total);

        return result;
    }

    private static EquipmentType mapType(String netxmsType) {
        if (netxmsType != null && netxmsType.trim().equalsIgnoreCase("Switch")) {
            return EquipmentType.COMMUTATEUR;
        }
        return EquipmentType.BORNE_WIFI;
    }

    private record EquipementRow(int objectId, int siteadminId, String name, String type) {}
}