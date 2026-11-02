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
        String actionMessage,   // null si tout fonctionne
        ContactDsiDto contactDsi   // peut etre null si aucun contact renseigne
) {
    // Un record peut contenir un AUTRE record imbrique, pour representer
    // une sous-structure (ici : le detail d'un etage).
    public record FloorStatusDto(
            String etage,
            NodeStatus status,
            int actifs,
            int total,
            String detail,
            List<EquipmentDetailDto> equipements,   // resume (statut/nom/type) de chaque equipement de l'etage
            // Tableau de suivi des amendements (items #16/#17) : seul
            // l'equipement PRINCIPAL d'un etage/batiment doit afficher ses
            // informations detaillees (vendor/emplacement/trafic) - pas
            // chaque equipement secondaire. Premier commutateur de l'etage,
            // a defaut premier equipement (meme principe de priorite que
            // AnpticStatusService pour le routeur WAN). Null si l'etage n'a
            // aucun equipement.
            EquipmentDetailDto equipementPrincipal
    ) {}

    public record EquipmentDetailDto(
            Long id,
            String libelleAffiche,
            String type,
            NodeStatus status,
            // Informations detaillees (section IV du compte rendu des
            // amendements : "consultation des informations detaillees de
            // tous les equipements rattaches a chaque batiment"). Issues de
            // netxmsdb (geo_equipement) quand disponibles - null sinon
            // (equipement non retrouve cote NetXMS, ex: objectId absent).
            // Jamais le nom technique NetXMS brut (§4.4 du CDC).
            String vendor,
            String modele,
            String emplacement,
            String trafficEntrant,
            String trafficSortant
    ) {}

    public record ContactDsiDto(
            String nom,
            String email,
            String telephone
    ) {}
}