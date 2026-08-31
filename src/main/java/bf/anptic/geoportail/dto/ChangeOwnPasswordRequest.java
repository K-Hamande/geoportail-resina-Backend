package bf.anptic.geoportail.dto;

// Recu depuis la page "Mon profil" : un utilisateur Backoffice change son
// PROPRE mot de passe. A la difference de ResetPasswordRequest (reserve a
// un SUPER_ADMIN qui reinitialise le mot de passe d'un AUTRE compte), on
// exige ici en plus la saisie du mot de passe actuel, par securite.
public record ChangeOwnPasswordRequest(
        String motDePasseActuel,
        String nouveauMotDePasse
) {}