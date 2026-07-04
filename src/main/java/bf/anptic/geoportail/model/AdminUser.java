package bf.anptic.geoportail.model;

import jakarta.persistence.*;

// Compte administrateur du Backoffice (agent DEST/DIG), §3.2.6b du CDC.
// Distinct de l'acces "Decideur" (qui utilise un simple token, cf. Etape 8).
@Entity
@Table(name = "admin_users")
public class AdminUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String login;

    private String nomComplet;

    // On ne stocke JAMAIS un mot de passe en clair : on stocke son "hash"
    // (une empreinte a sens unique, calculee par un algorithme comme BCrypt).
    // Meme en cas de fuite de la base, le mot de passe reel reste protege.
    @Column(nullable = false)
    private String motDePasseHash;

    private Boolean actif;

    // ---- Getters et setters ----

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getNomComplet() {
        return nomComplet;
    }

    public void setNomComplet(String nomComplet) {
        this.nomComplet = nomComplet;
    }

    public String getMotDePasseHash() {
        return motDePasseHash;
    }

    public void setMotDePasseHash(String motDePasseHash) {
        this.motDePasseHash = motDePasseHash;
    }

    public Boolean getActif() {
        return actif;
    }

    public void setActif(Boolean actif) {
        this.actif = actif;
    }
}