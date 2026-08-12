package bf.anptic.geoportail.dto;

import java.util.List;

// Vue temps reel "equipements + debit + disponibilite" pour un site,
// alimentee directement depuis netxmsdb (vues geo_equipement / geo_disponibilite).
public record SiteNetworkDto(
        String siteId,
        List<EquipmentNetworkDto> equipements
) {
    public record EquipmentNetworkDto(
            Integer objectId,
            String nom,
            String statut,          // statut brut NetXMS (severite numerique en texte)
            String vendor,
            String modele,
            String type,
            String emplacement,
            String trafficEntrant,  // ex: "29 Mb/s"
            String trafficSortant,
            Double disponibilitePourcentage,   // % sur la journee en cours, null si non calcule
            Long nombreIncidents
    ) {}
}