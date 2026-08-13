package bf.anptic.geoportail.dto;

import bf.anptic.geoportail.model.enums.NodeStatus;

import java.util.List;

// Conforme au §3.2.2 du CDC : compteurs globaux + detail par etage.
public record LanStatusDto(
        String siteId,
        NodeStatus globalStatus,
        int equipementsActifs,
        int equipementsTotal,
        int equipementsEnPanne,
        List<FloorStatusDto> etats,
        String message,
        String actionMessage   // null si tout fonctionne
) {
    // Un record peut contenir un AUTRE record imbrique, pour representer
    // une sous-structure (ici : le detail d'un etage).
    public record FloorStatusDto(
            String etage,
            NodeStatus status,
            int actifs,
            int total,
            String detail,
            List<EquipmentDetailDto> equipements   // detail deplie cote frontend
    ) {}

    public record EquipmentDetailDto(
            Long id,
            String libelleAffiche,
            String type,
            NodeStatus status
    ) {}
}