package bf.anptic.geoportail.dto;

public record EquipmentResponse(
        Long id,
        String siteId,
        String etageLabel,
        String type,
        String libelleAffiche,
        Integer netxmsObjectId
) {}