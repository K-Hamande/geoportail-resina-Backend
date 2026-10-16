package bf.anptic.geoportail.dto;

import java.time.Instant;

// Un ticket d'incident iTop (classe Ticket/Incident), tel qu'affiche au
// decideur sur la page "Alertes" - remplace l'ancien calcul automatique
// depuis NetXMS pour cet ecran precis (cf. IncidentDto, toujours utilise
// par ailleurs : historique backoffice, alertes email temps reel).
//
// PAS de siteId/ville ici : contrairement aux statuts ANPTIC/LAN, les
// tickets iTop ne sont lies de façon fiable qu'a un MINISTERE (champ
// org_name). Le site concerne n'existe, pour l'instant, qu'en texte libre
// dans la description du ticket (formats incoherents, aucun champ
// Location/CI renseigne) - pas de base fiable pour un siteId structure.
public record TicketIncidentDto(
        String ref,
        String titre,
        String ministere,
        String statut,        // valeur iTop brute : "ongoing", "resolved" ou "closed"
        Instant survenuLe,
        Instant clotureLe      // null si pas encore cloture
) {}
