package bf.anptic.geoportail.dto;

import bf.anptic.geoportail.model.enums.NodeStatus;

import java.time.Instant;

// Un "incident" = un probleme actuellement actif sur un site (ANPTIC ou
// LAN), calcule a la volee depuis les statuts en temps reel - il n'existe
// pas de table d'historique dediee pour l'instant, donc cette liste
// reflete l'etat ACTUEL du reseau, pas un historique date avec precision
// pour chaque panne (seule la date de panne ANPTIC est fiable, fournie
// par NetXMS ; celle du LAN est approximee a l'instant de calcul).
public record IncidentDto(
        String id,             // stable : "{siteId}-anptic" ou "{siteId}-lan"
        String type,           // "ANPTIC" ou "LAN"
        String siteId,
        String siteNom,
        String ville,
        String ministere,      // ministere proprietaire du site (peut etre null) -
                                // sert a cibler les alertes email vers les decideurs
                                // de ce ministere (cf. IncidentAlertScheduler)
        NodeStatus nouveauStatut,
        String message,
        Instant survenuLe
) {}