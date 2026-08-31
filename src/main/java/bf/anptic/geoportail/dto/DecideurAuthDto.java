package bf.anptic.geoportail.dto;

// DTOs pour l'authentification decideur

public class DecideurAuthDto {

    // Requete de connexion
    public record LoginRequest(String login, String motDePasse) {}

    // Reponse apres connexion reussie : token JWT + infos du profil
    public record LoginResponse(
            String token,
            String nomComplet,
            String role,        // "DECIDEUR" ou "LAMBDA"
            String ministere    // null si LAMBDA
    ) {}

    // Reponse backoffice : liste des comptes decideurs
    public record DecideurUserResponse(
            Long id,
            String login,
            String nomComplet,
            String email,       // pour l'envoi des alertes email ciblees - peut etre null
            String role,
            String ministere,
            Boolean actif,
            Boolean alertesActivees, // preference geree par le decideur lui-meme, lecture seule ici
            java.time.Instant creeLe
    ) {}

    // Requete backoffice : creation/modification d'un compte decideur
    public record DecideurUserRequest(
            String login,
            String nomComplet,
            String email,       // optionnel - sans email, pas d'alerte ciblee pour ce compte
            String motDePasse,  // null si modification sans changer le mdp
            String role,
            String ministere
    ) {}
}