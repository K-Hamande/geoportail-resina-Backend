package bf.anptic.geoportail.dto;

import java.util.Set;

// Recu depuis le Backoffice pour creer un nouveau compte administrateur
// (§3.2.6b : "creation de comptes administrateurs, attribution de droits
// par site ou globaux").
public record AdminUserCreateRequest(
        String login,
        String nomComplet,
        String motDePasse,
        String role,                  // "SUPER_ADMIN" / "ADMIN_DEST" / "ADMIN_DIG"
        Set<String> sitesAutorises    // vide/absent = acces global
) {}