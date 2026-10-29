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
        // Niveau normalise ("OK"/"WARN"/"KO") associe a qualiteSignal, pour
        // que le frontend puisse colorer le libelle sans avoir a re-parser
        // le texte non technique (cf. AnpticStatusService.calculerQualite).
        String qualiteNiveau,
        Double latenceMs,
        // Deja calcule et utilise dans le score de qualite depuis le debut
        // (cf. AnpticStatusService.calculerQualite), mais jamais expose au
        // frontend jusqu'ici - ajoute pour le compte rendu des amendements,
        // section IV ("Perte de paquets" en complement de qualite/debit/latence).
        Double perteDePaquetsPct,
        Double disponibilite30Jours,
        Instant indisponibleDepuis,
        String actionMessage,
        // Informations detaillees de l'equipement PRINCIPAL du site (le
        // routeur si identifie, sinon le premier equipement rattache -
        // meme logique que le choix de l'equipement utilise pour calculer
        // tous les indicateurs ci-dessus). Jamais le nom technique NetXMS
        // brut (§4.4 du CDC) - vendor/modele/emplacement uniquement.
        EquipementPrincipalDto equipementPrincipal
) {
    public record EquipementPrincipalDto(
            String vendor,
            String modele,
            String emplacement
    ) {}
}