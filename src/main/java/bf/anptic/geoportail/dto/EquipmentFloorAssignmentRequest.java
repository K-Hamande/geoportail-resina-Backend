package bf.anptic.geoportail.dto;

// Le backoffice ne fait que RATTACHER un etage et/ou un libelle affiche
// a un equipement deja synchronise depuis NetXMS - jamais en creer/
// supprimer. libelleAffiche est optionnel : laisse a null/vide, un
// libelle generique ("Borne Wi-Fi 1"...) est utilise a l'affichage
// (voir LanStatusService).
public record EquipmentFloorAssignmentRequest(
        String etageLabel,
        String libelleAffiche
) {}