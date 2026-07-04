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