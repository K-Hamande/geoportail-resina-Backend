package bf.anptic.geoportail.service;

import bf.anptic.geoportail.dto.LanStatusDto;
import bf.anptic.geoportail.dto.LanStatusDto.EquipmentDetailDto;
import bf.anptic.geoportail.dto.LanStatusDto.FloorStatusDto;
import bf.anptic.geoportail.model.Equipment;
import bf.anptic.geoportail.model.Site;
import bf.anptic.geoportail.model.enums.NodeStatus;
import bf.anptic.geoportail.repository.EquipmentRepository;
import bf.anptic.geoportail.repository.SiteRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class LanStatusService {

    // object_id est directement dans object_properties (deja accessible
    // en integralite par geoportail_readonly). On ne lit ici QUE le statut,
    // par lots via IN (:ids), pour tous les equipements LAN declares.
    private static final String SELECT_STATUTS = """
            SELECT object_id, status
            FROM public.object_properties
            WHERE object_id IN (:ids)
            """;

    private final SiteRepository siteRepository;
    private final EquipmentRepository equipmentRepository;
    private final NamedParameterJdbcTemplate netxmsNamedJdbcTemplate;

    public LanStatusService(SiteRepository siteRepository,
                             EquipmentRepository equipmentRepository,
                             @Qualifier("netxmsNamedJdbcTemplate") NamedParameterJdbcTemplate netxmsNamedJdbcTemplate) {
        this.siteRepository = siteRepository;
        this.equipmentRepository = equipmentRepository;
        this.netxmsNamedJdbcTemplate = netxmsNamedJdbcTemplate;
    }

    public LanStatusDto getLanStatus(String siteId) {
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Site introuvable : " + siteId));

        // 1) Tous les equipements DECLARES pour ce site (venant de notre base)
        List<Equipment> equipments = equipmentRepository.findBySite_SiteId(siteId);

        // 2) Leur statut REEL, recupere depuis netxmsdb (par lots)
        Map<Integer, Integer> statusByObjectId = fetchStatuses(equipments);

        // 3) Regrouper les equipements par etage.
        Map<String, List<Equipment>> byFloor = new LinkedHashMap<>();
        for (Equipment eq : equipments) {
            String etage = eq.getEtageLabel() != null ? eq.getEtageLabel() : "Non assigné";
            byFloor.computeIfAbsent(etage, k -> new ArrayList<>()).add(eq);
        }

        List<FloorStatusDto> floorStatuses = new ArrayList<>();
        int totalActifs = 0;
        int total = equipments.size();
        NodeStatus worstStatus = NodeStatus.OK;

        for (Map.Entry<String, List<Equipment>> entry : byFloor.entrySet()) {
            String etage = entry.getKey();
            List<Equipment> floorEquipments = entry.getValue();

            int floorTotal = floorEquipments.size();
            int floorActifs = 0;
            List<EquipmentDetailDto> details = new ArrayList<>();

            for (Equipment eq : floorEquipments) {
                Integer rawStatus = statusByObjectId.get(eq.getNetxmsObjectId());
                NodeStatus eqStatus = NodeStatus.fromNetXmsSeverityCode(rawStatus);
                boolean up = eqStatus == NodeStatus.OK;
                if (up) {
                    floorActifs++;
                }

                details.add(new EquipmentDetailDto(
                        eq.getId(),
                        eq.getLibelleAffiche(),
                        eq.getType() != null ? eq.getType().name() : null,
                        eqStatus
                ));
            }

            totalActifs += floorActifs;
            int floorPannes = floorTotal - floorActifs;

            NodeStatus floorStatus = floorPannes == 0 ? NodeStatus.OK
                    : (floorPannes >= floorTotal ? NodeStatus.KO : NodeStatus.WARN);
            worstStatus = NodeStatus.worstOf(worstStatus, floorStatus);

            String detail = floorPannes == 0
                    ? "Tout fonctionne · " + floorActifs + " équipements sur " + floorTotal + " actifs"
                    : floorPannes + " équipement(s) hors service sur " + floorTotal;

            floorStatuses.add(new FloorStatusDto(etage, floorStatus, floorActifs, floorTotal, detail, details));
        }

        int pannes = total - totalActifs;
        String message = pannes == 0 ? "Aucun problème dans le bâtiment" : "Des équipements sont hors service";
        String actionMessage = pannes == 0 ? null : "Veuillez en informer votre DSI";

        return new LanStatusDto(siteId, worstStatus, totalActifs, total, pannes, floorStatuses, message, actionMessage);
    }

    private Map<Integer, Integer> fetchStatuses(List<Equipment> equipments) {
        List<Integer> ids = equipments.stream()
                .map(Equipment::getNetxmsObjectId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (ids.isEmpty()) {
            return Map.of();
        }

        Map<Integer, Integer> result = new HashMap<>();
        netxmsNamedJdbcTemplate.query(SELECT_STATUTS, new MapSqlParameterSource("ids", ids), rs -> {
            result.put(rs.getInt("object_id"), (Integer) rs.getObject("status"));
        });
        return result;
    }
}