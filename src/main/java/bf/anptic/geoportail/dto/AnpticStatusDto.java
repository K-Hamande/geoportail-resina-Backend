package bf.anptic.geoportail.dto;

import bf.anptic.geoportail.model.enums.NodeStatus;

import java.time.Instant;

// Un "record" declare directement, entre parentheses, la liste de ses
// champs (ici en lecture seule). Spring/Jackson saura automatiquement
// le convertir en JSON pour la reponse HTTP.
//
// Conforme au §3.2.1 du CDC : langage non technique, montant/descendant,
// type de liaison, qualite, latence, disponibilite 30j, message d'action.
public record AnpticStatusDto(
        String siteId,
        NodeStatus status,
        boolean disponible,
        String message,
        Double debitMontantMbps,
        Double debitDescendantMbps,
        String typeLiaison,
        String qualiteSignal,
        Double latenceMs,
        Double disponibilite30Jours,
        Instant indisponibleDepuis,
        String actionMessage
) {}