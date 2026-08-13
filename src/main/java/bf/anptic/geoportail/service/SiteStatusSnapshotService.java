package bf.anptic.geoportail.service;

import bf.anptic.geoportail.model.Equipment;
import bf.anptic.geoportail.model.Site;
import bf.anptic.geoportail.model.enums.NodeStatus;
import bf.anptic.geoportail.repository.EquipmentRepository;
import bf.anptic.geoportail.repository.SiteRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

// Calcule le statut (ANPTIC + LAN) de TOUS les sites actifs EN UNE SEULE
// PASSE (4 requetes SQL au total, quel que soit le nombre de sites),
// plutot que d'appeler AnpticStatusService/LanStatusService site par site
// (qui, eux, sont optimises pour l'affichage d'UN site, pas pour un
// calcul en masse - les appeler en boucle sur 1600+ sites provoquerait
// des milliers de requetes sequentielles, beaucoup trop lent).
//
// Reutilise par MapService (carte) et IncidentService (alertes).
@Service
public class SiteStatusSnapshotService {

    // Pour chaque site, on prend l'equipement "Routeur" (liaison WAN),
    // ou a defaut le premier equipement du site (meme regle que
    // AnpticStatusService, mais ici calculee pour TOUS les sites d'un
    // coup grace a DISTINCT ON).
    private static final String SELECT_ANPTIC_BULK = """
            SELECT DISTINCT ON (ge.siteadmin_id)
                   ge.siteadmin_id, ge.status, ge.technologie, n.down_since
            FROM public.geo_equipement ge
            JOIN public.nodes n ON n.id = ge.object_id
            ORDER BY ge.siteadmin_id,
                     CASE WHEN ge.type ILIKE '%rout%' THEN 0 ELSE 1 END,
                     ge.object_id
            """;

    private static final String SELECT_STATUTS_EQUIPEMENTS = """
            SELECT object_id, status
            FROM public.object_properties
            WHERE object_id IN (:ids)
            """;

    private final SiteRepository siteRepository;
    private final EquipmentRepository equipmentRepository;
    private final JdbcTemplate netxmsJdbcTemplate;
    private final NamedParameterJdbcTemplate netxmsNamedJdbcTemplate;

    public SiteStatusSnapshotService(SiteRepository siteRepository,
                                      EquipmentRepository equipmentRepository,
                                      @Qualifier("netxmsJdbcTemplate") JdbcTemplate netxmsJdbcTemplate,
                                      @Qualifier("netxmsNamedJdbcTemplate") NamedParameterJdbcTemplate netxmsNamedJdbcTemplate) {
        this.siteRepository = siteRepository;
        this.equipmentRepository = equipmentRepository;
        this.netxmsJdbcTemplate = netxmsJdbcTemplate;
        this.netxmsNamedJdbcTemplate = netxmsNamedJdbcTemplate;
    }

    public List<SiteStatusSnapshot> computeAll() {
        List<Site> sites = siteRepository.findByActifTrue();

        // 1) Statut ANPTIC (WAN) de tous les sites, en une requete.
        Map<Integer, AnpticRow> anpticBySiteAdminId = new HashMap<>();
        netxmsJdbcTemplate.query(SELECT_ANPTIC_BULK, rs -> {
            int siteAdminId = rs.getInt("siteadmin_id");
            anpticBySiteAdminId.put(siteAdminId, new AnpticRow(
                    (Integer) rs.getObject("status"),
                    rs.getString("technologie"),
                    rs.getLong("down_since")
            ));
        });

        // 2) Equipements LAN declares localement, regroupes par site.
        Map<String, List<Equipment>> equipmentsBySiteId = new HashMap<>();
        for (Equipment eq : equipmentRepository.findAll()) {
            if (eq.getSite() == null) continue;
            equipmentsBySiteId.computeIfAbsent(eq.getSite().getSiteId(), k -> new ArrayList<>()).add(eq);
        }

        // 3) Statut REEL de tous ces equipements, en une requete groupee.
        List<Integer> tousLesObjectIds = equipmentsBySiteId.values().stream()
                .flatMap(List::stream)
                .map(Equipment::getNetxmsObjectId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Integer, Integer> statutParObjectId = new HashMap<>();
        if (!tousLesObjectIds.isEmpty()) {
            netxmsNamedJdbcTemplate.query(SELECT_STATUTS_EQUIPEMENTS,
                    new MapSqlParameterSource("ids", tousLesObjectIds), rs -> {
                        statutParObjectId.put(rs.getInt("object_id"), (Integer) rs.getObject("status"));
                    });
        }

        // 4) Assemblage final, site par site, uniquement en memoire (plus
        // aucun acces base ici).
        List<SiteStatusSnapshot> snapshots = new ArrayList<>();
        for (Site site : sites) {
            snapshots.add(buildSnapshot(site, anpticBySiteAdminId, equipmentsBySiteId, statutParObjectId));
        }
        return snapshots;
    }

    private SiteStatusSnapshot buildSnapshot(Site site,
                                              Map<Integer, AnpticRow> anpticBySiteAdminId,
                                              Map<String, List<Equipment>> equipmentsBySiteId,
                                              Map<Integer, Integer> statutParObjectId) {
        NodeStatus anpticStatus = NodeStatus.UNKNOWN;
        String technologie = null;
        Instant indisponibleDepuis = null;

        if (site.getNetxmsNodeId() != null) {
            AnpticRow row = anpticBySiteAdminId.get(site.getNetxmsNodeId());
            if (row != null) {
                anpticStatus = NodeStatus.fromNetXmsSeverityCode(row.status());
                technologie = row.technologie();
                if (anpticStatus != NodeStatus.OK && row.downSince() != 0) {
                    indisponibleDepuis = Instant.ofEpochSecond(row.downSince());
                }
            }
        }

        List<Equipment> equipements = equipmentsBySiteId.getOrDefault(site.getSiteId(), List.of());
        int total = equipements.size();
        int actifs = 0;
        for (Equipment eq : equipements) {
            Integer rawStatus = statutParObjectId.get(eq.getNetxmsObjectId());
            if (NodeStatus.fromNetXmsSeverityCode(rawStatus) == NodeStatus.OK) {
                actifs++;
            }
        }
        int pannes = total - actifs;
        NodeStatus lanStatus = total == 0 ? NodeStatus.OK
                : pannes == 0 ? NodeStatus.OK
                : pannes >= total ? NodeStatus.KO
                : NodeStatus.WARN;

        return new SiteStatusSnapshot(site, anpticStatus, technologie, indisponibleDepuis, lanStatus, pannes, total);
    }

    private record AnpticRow(Integer status, String technologie, long downSince) {}
}