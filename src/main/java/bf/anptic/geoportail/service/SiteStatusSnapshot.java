package bf.anptic.geoportail.service;

import bf.anptic.geoportail.model.Site;
import bf.anptic.geoportail.model.enums.NodeStatus;

import java.time.Instant;

// "Photo" du statut d'UN site, calculee en masse pour TOUS les sites a la
// fois (voir SiteStatusSnapshotService) - reutilisee a la fois par la
// carte (/sites/map) et par les alertes (/incidents), pour eviter de
// recalculer deux fois la meme chose.
public record SiteStatusSnapshot(
        Site site,
        NodeStatus anpticStatus,
        String anpticTechnologie,
        Instant anpticIndisponibleDepuis,   // null si statut OK ou donnee absente
        NodeStatus lanStatus,
        int lanEquipementsEnPanne,
        int lanEquipementsTotal
) {
    public NodeStatus statutGlobal() {
        return NodeStatus.worstOf(anpticStatus, lanStatus);
    }
}