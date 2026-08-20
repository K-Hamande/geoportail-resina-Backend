
// Statistiques globales des equipements pour le tableau de bord
// Backoffice (§3.2.6b) : total, repartition par type, et listes de
// valeurs distinctes pour alimenter les menus deroulants de filtres.
package bf.anptic.geoportail.dto;

import java.util.List;

public record EquipmentStatsDto(
        long total,
        List<TypeStatDto> parType,
        List<String> regions,
        List<String> provinces,
        List<String> villes,
        List<String> ministeres,
        List<String> structures
) {
    public record TypeStatDto(
            String type,
            String libelle,
            long count,
            double pourcentage
    ) {}
}