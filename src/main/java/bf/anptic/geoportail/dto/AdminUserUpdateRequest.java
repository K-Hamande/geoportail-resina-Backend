package bf.anptic.geoportail.dto;

import java.util.Set;

// Recu depuis le Backoffice pour modifier un compte administrateur DEJA
// EXISTANT (nom, role, sites autorises) - jamais le mot de passe (voir
// ResetPasswordRequest pour ca) ni le login (identifiant stable, non
// modifiable une fois le compte cree).
public record AdminUserUpdateRequest(
        String nomComplet,
        String role,                  // "SUPER_ADMIN" / "ADMIN_DEST" / "ADMIN_DIG"
        Set<String> sitesAutorises    // vide/absent = acces global
) {}