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
        Integer niveaux,
        // Jusqu'ici renseignes UNIQUEMENT par la synchro NetXMS
        // (NetxmsSiteImportService), jamais par une creation/modification
        // manuelle ni par import CSV - ajoutes pour l'import CSV de sites
        // (section VI du compte rendu des amendements), sans quoi un site
        // importe par ce biais n'aurait ni ministere ni province renseignes.
        String ministere,
        String province
) {}