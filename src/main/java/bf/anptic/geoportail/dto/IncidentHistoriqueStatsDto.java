package bf.anptic.geoportail.dto;

// Statistiques affichees en haut de la page "Historique des incidents" -
// calculees sur le MEME ensemble filtre que la liste (periode/date, type,
// ministere, recherche), mais AVANT d'appliquer le filtre "etat" et la
// pagination : resolus + enCours == total, quel que soit l'onglet
// (Tous/En cours/Resolus) actuellement affiche.
//
// ko/warn/inconnu : nombre d'incidents dont le DERNIER statut connu (voir
// IncidentHistorique.statut) est respectivement KO, WARN ou UNKNOWN - sur
// le meme ensemble filtre, donc ko + warn + inconnu == total (un incident
// resolu garde le statut qu'il avait juste avant sa resolution).
//
// BUG CORRIGE (retour utilisateur du 30/10/2026) : avant l'ajout du champ
// "inconnu", un incident dont le dernier statut connu etait UNKNOWN
// (possible : IncidentService.listIncidents() n'exclut que OK, pas
// UNKNOWN - cf. SiteStatusSnapshotService/AnpticStatusService, qui
// peuvent renvoyer UNKNOWN faute de donnee NetXMS) etait compte dans
// "total" mais NI dans "ko" NI dans "warn", cassant silencieusement
// l'invariant ko + warn == total documente plus haut.
public record IncidentHistoriqueStatsDto(
        long total,
        long resolus,
        long enCours,
        long ko,
        long warn,
        long inconnu,
        // Section V du compte rendu des amendements : taux de disponibilite/
        // indisponibilite sur la PERIODE FILTREE (pas 30 jours fixes comme
        // AnpticStatusDto.disponibilite30Jours, cote decideur) - voir
        // IncidentHistoryService.calculerDisponibilite pour le detail du calcul.
        // Null si aucun site actif ne correspond au filtre (division impossible).
        Double disponibilitePct,
        Double indisponibilitePct
) {}