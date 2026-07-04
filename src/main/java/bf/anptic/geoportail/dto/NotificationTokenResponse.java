package bf.anptic.geoportail.dto;

import java.time.Instant;

// Renvoye cote BACKOFFICE. Le token n'est jamais renvoye en entier
// (tokenMasque n'affiche que les 4 derniers caracteres) - ce n'est pas
// une donnee "secrete" comme un mot de passe, mais il n'y a aucune
// raison de l'exposer entier a l'affichage.
public record NotificationTokenResponse(
        Long id,
        String siteId,
        String siteNom,
        String profil,
        String plateforme,
        String tokenMasque,
        Boolean actif,
        Instant enregistreLe
) {}