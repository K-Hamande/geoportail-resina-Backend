package bf.anptic.geoportail.dto;

// DTO recu depuis le Backoffice pour declarer un equipement LAN
// rattache a un etage d'un site (§3.2.6a du CDC).
public record EquipmentAdminRequest(
        String etageLabel,
        String type,             // "BORNE_WIFI" ou "COMMUTATEUR"
        String libelleAffiche,
        Integer netxmsObjectId
) {}