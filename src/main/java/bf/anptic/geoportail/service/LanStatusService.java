package bf.anptic.geoportail.service;

import bf.anptic.geoportail.client.NetXmsClient;
import bf.anptic.geoportail.dto.LanStatusDto;
import bf.anptic.geoportail.dto.LanStatusDto.FloorStatusDto;
import bf.anptic.geoportail.model.Equipment;
import bf.anptic.geoportail.model.Site;
import bf.anptic.geoportail.model.enums.NodeStatus;
import bf.anptic.geoportail.repository.EquipmentRepository;
import bf.anptic.geoportail.repository.SiteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class LanStatusService {

    private final SiteRepository siteRepository;
    private final EquipmentRepository equipmentRepository;
    private final NetXmsClient netXmsClient;

    public LanStatusService(SiteRepository siteRepository,
                             EquipmentRepository equipmentRepository,
                             NetXmsClient netXmsClient) {
        this.siteRepository = siteRepository;
        this.equipmentRepository = equipmentRepository;
        this.netXmsClient = netXmsClient;
    }

    public LanStatusDto getLanStatus(String siteId) {
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Site introuvable : " + siteId));

        // 1) Tous les equipements DECLARES pour ce site (venant de notre base)
        List<Equipment> equipments = equipmentRepository.findBySite_SiteId(siteId);

        // 2) Leur statut REEL, recupere depuis NetXMS
        Map<Integer, String> statusByObjectId = netXmsClient.getChildrenStatuses(site.getNetxmsNodeId());

        // 3) Regrouper les equipements par etage.
        // Map<String, List<Equipment>> = pour chaque nom d'etage (cle),
        // la liste des equipements qui s'y trouvent (valeur).
        Map<String, List<Equipment>> byFloor = new LinkedHashMap<>();
        for (Equipment eq : equipments) {
            // computeIfAbsent : si la cle "etageLabel" n'existe pas encore
            // dans la map, on cree une liste vide avant d'y ajouter l'equipement.
            byFloor.computeIfAbsent(eq.getEtageLabel(), k -> new ArrayList<>()).add(eq);
        }

        List<FloorStatusDto> floorStatuses = new ArrayList<>();
        int totalActifs = 0;
        int total = equipments.size();
        NodeStatus worstStatus = NodeStatus.OK;

        // 4) Calculer, POUR CHAQUE ETAGE, le nombre d'actifs/pannes
        for (Map.Entry<String, List<Equipment>> entry : byFloor.entrySet()) {
            String etage = entry.getKey();
            List<Equipment> floorEquipments = entry.getValue();

            int floorTotal = floorEquipments.size();
            int floorActifs = 0;

            for (Equipment eq : floorEquipments) {
                String rawStatus = statusByObjectId.get(eq.getNetxmsObjectId());
                boolean up = NodeStatus.fromNetXmsSeverity(rawStatus) == NodeStatus.OK;
                if (up) {
                    floorActifs++;
                }
            }

            totalActifs += floorActifs;
            int floorPannes = floorTotal - floorActifs;

            NodeStatus floorStatus = floorPannes == 0 ? NodeStatus.OK
                    : (floorPannes >= floorTotal ? NodeStatus.KO : NodeStatus.WARN);
            // On garde le "pire" statut rencontre, pour le statut global du site
            worstStatus = NodeStatus.worstOf(worstStatus, floorStatus);

            String detail = floorPannes == 0
                    ? "Tout fonctionne · " + floorActifs + " équipements sur " + floorTotal + " actifs"
                    : floorPannes + " équipement(s) hors service sur " + floorTotal;

            floorStatuses.add(new FloorStatusDto(etage, floorStatus, floorActifs, floorTotal, detail));
        }

        int pannes = total - totalActifs;
        String message = pannes == 0 ? "Aucun problème dans le bâtiment" : "Des équipements sont hors service";
        String actionMessage = pannes == 0 ? null : "Veuillez en informer votre DSI";

        return new LanStatusDto(siteId, worstStatus, totalActifs, total, pannes, floorStatuses, message, actionMessage);
    }

    // // OK < WARN < KO en gravite : sert a determiner le "pire" statut
    // private static int severityRank(NodeStatus status) {
    //     return switch (status) {
    //         case OK -> 0;
    //         case WARN, UNKNOWN -> 1;
    //         case KO -> 2;
    //     };
    // }
}