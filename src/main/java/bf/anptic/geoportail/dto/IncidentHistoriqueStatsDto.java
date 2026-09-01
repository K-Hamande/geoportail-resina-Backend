package bf.anptic.geoportail.dto;

// Statistiques affichees en haut de la page "Historique des incidents" -
// calculees sur le MEME ensemble filtre que la liste (periode/date, type,
// ministere, recherche), mais AVANT d'appliquer le filtre "etat" et la
// pagination : resolus + enCours == total, quel que soit l'onglet
// (Tous/En cours/Resolus) actuellement affiche.
//
// ko/warn : nombre d'incidents dont le DERNIER statut connu (voir
// IncidentHistorique.statut) est respectivement KO ou WARN - sur le meme
// ensemble filtre, donc ko + warn == total (un incident resolu garde le
// statut qu'il avait juste avant sa resolution).
public record IncidentHistoriqueStatsDto(
        long total,
        long resolus,
        long enCours,
        long ko,
        long warn
) {}