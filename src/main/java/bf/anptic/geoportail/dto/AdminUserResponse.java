package bf.anptic.geoportail.dto;

import java.util.Set;

// Renvoye au client : ne contient JAMAIS le mot de passe ni son hash.
public record AdminUserResponse(
        Long id,
        String login,
        String nomComplet,
        String role,
        Set<String> sitesAutorises,
        Boolean actif
) {}