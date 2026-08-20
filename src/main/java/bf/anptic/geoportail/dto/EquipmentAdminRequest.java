package bf.anptic.geoportail.dto;

public record EquipmentAdminRequest(
        String etageLabel,
        String type,
        String libelleAffiche,
        Integer netxmsObjectId
) {}