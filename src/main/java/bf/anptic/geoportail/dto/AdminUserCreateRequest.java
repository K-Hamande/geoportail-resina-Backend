package bf.anptic.geoportail.dto;

// Recu depuis le Backoffice pour creer un nouveau compte administrateur
// (§3.2.6b : "creation de comptes administrateurs, reinitialisation
// des mots de passe").
public record AdminUserCreateRequest(
        String login,
        String nomComplet,
        String motDePasse   // en clair, envoye UNE FOIS a la creation, jamais stocke tel quel
) {}