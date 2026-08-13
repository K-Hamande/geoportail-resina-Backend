package bf.anptic.geoportail.dto;

// DTO recu depuis le Backoffice pour creer ou modifier un site
// (§3.2.6a du CDC : nom, localisation, contact DSI, netxms_node_id...).
public record SiteAdminRequest(
        String siteId,
        String nom,
        String ville,
        String regionAdministrative,
        String batiment,
        Double latitude,
        Double longitude,
        String contactDsiNom,
        String contactDsiTelephone,
        String contactDsiEmail,
        Integer netxmsNodeId,
        Integer niveaux
) {}