package bf.anptic.geoportail.dto;

import java.time.Instant;

// Une ligne de la page Backoffice "Historique des incidents". A la
// difference de IncidentDto (etat instantane, calcule a la volee), ceci
// vient de la table incident_historique : debutLe est fiable (pose une
// seule fois, jamais recalcule), et finLe permet de savoir quand la panne
// a ete resolue (null = toujours en cours).
public record IncidentHistoriqueDto(
        Long id,
        String type,
        String siteId,
        String siteNom,
        String ville,
        String ministere,
        String statut,
        String message,
        Instant debutLe,
        Instant finLe,
        boolean enCours,
        long dureeMinutes,
        // Section V du compte rendu des amendements : agregats calcules
        // pour LE SITE de cette ligne, sur la meme fenetre/les memes
        // filtres (date/type/ministere/recherche) que le reste de la
        // page - pas sur la duree de vie complete du site, pour rester
        // coherent avec les KPI globaux affiches au-dessus du tableau.
        int nombreIncidentsSite,
        Double disponibiliteSitePct
) {}