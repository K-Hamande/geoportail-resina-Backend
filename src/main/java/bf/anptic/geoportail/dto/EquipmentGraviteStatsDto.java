package bf.anptic.geoportail.dto;

// Repartition de tous les equipements par niveau de gravite (section II
// du compte rendu des amendements : Critique / Majeur / niveau
// intermediaire / Normal). Libelle valide par le tableau de suivi des
// amendements : "Moyen" (pas "Mineur") - le champ s'appelle
// "intermediaire" cote API (nom de code stable), le libelle "Moyen"
// n'est applique que cote UI.
public record EquipmentGraviteStatsDto(
        long normal,
        long intermediaire,
        long majeur,
        long critique,
        long inconnu
) {}
