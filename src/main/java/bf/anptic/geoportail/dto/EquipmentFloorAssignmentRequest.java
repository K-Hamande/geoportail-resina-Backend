package bf.anptic.geoportail.dto;

// Remplace l'ancien EquipmentAdminRequest (creation manuelle, retiree).
// Le backoffice ne fait plus que RATTACHER un etage a un equipement deja
// decouvert automatiquement depuis NetXMS - jamais en creer/supprimer.
public record EquipmentFloorAssignmentRequest(
        String etageLabel
) {}    