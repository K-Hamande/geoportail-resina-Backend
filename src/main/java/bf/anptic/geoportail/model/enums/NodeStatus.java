package bf.anptic.geoportail.model.enums;

public enum NodeStatus {
    OK,
    WARN,
    KO,
    UNKNOWN;

    public static NodeStatus fromNetXmsSeverity(String netXmsSeverity) {
        if (netXmsSeverity == null) {
            return UNKNOWN;
        }
        return switch (netXmsSeverity.trim().toUpperCase()) {
            case "NORMAL" -> OK;
            case "WARNING", "MINOR" -> WARN;
            case "MAJOR", "CRITICAL" -> KO;
            default -> UNKNOWN;
        };
    }

    // Variante pour les codes de statut NUMERIQUES tels que renvoyes par
    // netxmsdb (ex: geo_equipement.status), a la difference du texte
    // ("NORMAL", "CRITICAL"...) utilise par l'API REST NetXMS mockee.
    // Codes standards NetXMS : 0=Normal 1=Warning 2=Minor 3=Major
    // 4=Critical 5=Unknown 6=Unmanaged 7=Disabled 8=Testing.
    public static NodeStatus fromNetXmsSeverityCode(Integer code) {
        if (code == null) {
            return UNKNOWN;
        }
        return switch (code) {
            case 0 -> OK;
            case 1, 2 -> WARN;
            case 3, 4 -> KO;
            default -> UNKNOWN;
        };
    }

    // Renvoie un nombre representant la gravite : plus c'est eleve,
    // plus la situation est serieuse. Sert a determiner "le pire des
    // deux statuts" quand on combine plusieurs informations (ex: statut
    // ANPTIC + statut LAN -> statut global d'un site pour la carte).
    public int severityRank() {
        return switch (this) {
            case OK -> 0;
            case WARN, UNKNOWN -> 1;
            case KO -> 2;
        };
    }

    // Methode utilitaire statique : compare deux statuts et renvoie le
    // plus grave des deux.
    public static NodeStatus worstOf(NodeStatus a, NodeStatus b) {
        return b.severityRank() > a.severityRank() ? b : a;
    }
}