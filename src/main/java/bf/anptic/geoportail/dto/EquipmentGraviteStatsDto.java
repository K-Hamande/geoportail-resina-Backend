package bf.anptic.geoportail.dto;

// Repartition de tous les equipements par niveau de gravite (section II
// du compte rendu des amendements : Critique / Majeur / niveau
// intermediaire / Normal). Le libelle definitif du niveau intermediaire
// ("Moyen" ou "Mineur") reste a valider (section VIII) - le champ
// "intermediaire" ici est neutre, la decision du libelle se fait cote UI.
public record EquipmentGraviteStatsDto(
        long normal,
        long intermediaire,
        long majeur,
        long critique,
        long inconnu
) {}
