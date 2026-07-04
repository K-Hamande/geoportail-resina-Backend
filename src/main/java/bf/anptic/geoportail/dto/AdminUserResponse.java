package bf.anptic.geoportail.dto;

// Renvoye au client : ne contient JAMAIS le mot de passe ni son hash.
// C'est le point cle de ce DTO - le hash ne doit jamais quitter le backend.
public record AdminUserResponse(
        Long id,
        String login,
        String nomComplet,
        Boolean actif
) {}