package bf.anptic.geoportail.model.enums;

// Classification d'un SITE en 5 niveaux mutuellement exclusifs (amendement
// "Tableau de bord et gestion des sites" : remplace la notion generique de
// "site degrade" par Critique/Majeur/Moyen/Normal/Inconnu). Reutilise
// exactement le meme mapping que la repartition des equipements par
// gravite (section II, deja valide en production - voir
// EquipmentGraviteService) : codes de statut NetXMS 0=Normal, 1 et
// 2=Moyen, 3=Majeur, 4=Critique, tout le reste (ou absence de donnee)
// =Inconnu - applique ici au PIRE equipement du site (routeur WAN +
// equipements LAN), pas a un seul equipement isole.
public enum SiteGravite {
    NORMAL,
    MOYEN,
    MAJEUR,
    CRITIQUE,
    INCONNU;

    // Rang de gravite pour un code NetXMS CONNU (0 a 4) - null si le code
    // n'est pas reconnu (5=Unknown, 6=Unmanaged, 7=Disabled, 8=Testing,
    // ou tout autre valeur). Un code non reconnu ne degrade PAS le site
    // vers Inconnu a lui seul : il est simplement ignore dans le calcul
    // du pire equipement, Inconnu ne l'emportant que si AUCUN code valide
    // n'a ete trouve pour ce site (voir pireDe/ depuisCode ci-dessous).
    private static SiteGravite depuisCodeConnu(Integer code) {
        if (code == null) {
            return null;
        }
        return switch (code) {
            case 0 -> NORMAL;
            case 1, 2 -> MOYEN;
            case 3 -> MAJEUR;
            case 4 -> CRITIQUE;
            default -> null;
        };
    }

    private int rang() {
        return switch (this) {
            case NORMAL -> 0;
            case MOYEN, INCONNU -> 1;
            case MAJEUR -> 2;
            case CRITIQUE -> 3;
        };
    }

    // Combine le code d'UN equipement (routeur WAN ou equipement LAN) avec
    // la gravite deja retenue pour le site - ignore les codes non reconnus
    // (null depuis depuisCodeConnu) plutot que de faire basculer tout le
    // site en Inconnu a cause d'un seul equipement ambigu.
    public static SiteGravite combiner(SiteGravite graviteActuelle, Integer codeEquipement) {
        SiteGravite graviteCode = depuisCodeConnu(codeEquipement);
        if (graviteCode == null) {
            return graviteActuelle;
        }
        if (graviteActuelle == null) {
            return graviteCode;
        }
        return graviteCode.rang() > graviteActuelle.rang() ? graviteCode : graviteActuelle;
    }

    // A appeler une fois tous les equipements du site combines : si aucun
    // code valide n'a ete trouve (graviteActuelle reste null), le site est
    // Inconnu - conforme a l'amendement ("tout site dont le statut ne peut
    // pas etre determine doit etre classe dans Sites inconnus").
    public static SiteGravite finaliser(SiteGravite graviteActuelle) {
        return graviteActuelle != null ? graviteActuelle : INCONNU;
    }
}
