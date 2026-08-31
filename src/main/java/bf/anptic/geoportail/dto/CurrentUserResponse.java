package bf.anptic.geoportail.dto;

// Reponse de GET /backoffice/api/v1/me : identite du compte Backoffice
// actuellement connecte (page "Mon profil"). Ne contient jamais le mot de
// passe ni son hash.
public record CurrentUserResponse(
        Long id,
        String login,
        String nomComplet,
        String role
) {}