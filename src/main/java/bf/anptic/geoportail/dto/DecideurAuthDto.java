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
            String role,
            String ministere,
            Boolean actif,
            java.time.Instant creeLe
    ) {}

    // Requete backoffice : creation/modification d'un compte decideur
    public record DecideurUserRequest(
            String login,
            String nomComplet,
            String motDePasse,  // null si modification sans changer le mdp
            String role,
            String ministere
    ) {}
}