package bf.anptic.geoportail.model;

import jakarta.persistence.*;
import java.time.Instant;

// Compte utilisateur cote DECIDEUR (distinct des comptes Backoffice
// AdminUser). Deux roles :
//  - DECIDEUR : acces complet aux donnees de son ministere uniquement
//  - LAMBDA   : acces en consultation uniquement (statut global 🟢/🔴),
//               tous les sites visibles, pas de details techniques
@Entity
@Table(name = "decideur_users", schema = "geoportail_resina")
public class DecideurUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String login;
    private String nomComplet;
    private String email; // pour l'envoi des alertes email ciblees par ministere - peut etre null
    private String motDePasseHash;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String ministere; // null si role = LAMBDA
    private Boolean actif;

    // Preference personnelle du decideur : recevoir ou non les alertes
    // email pour les incidents de son ministere (cf. IncidentAlertScheduler).
    // Geree par le decideur lui-meme (bandeau "Activer les alertes" cote
    // frontend decideur), pas par le Backoffice.
    private Boolean alertesActivees;

    private Instant creeLe;
    private String creePar;

    public enum Role {
        DECIDEUR,   // acces complet, filtre par ministere
        LAMBDA      // acces consultation uniquement, tous sites
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public String getNomComplet() { return nomComplet; }
    public void setNomComplet(String nomComplet) { this.nomComplet = nomComplet; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getMotDePasseHash() { return motDePasseHash; }
    public void setMotDePasseHash(String motDePasseHash) { this.motDePasseHash = motDePasseHash; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getMinistere() { return ministere; }
    public void setMinistere(String ministere) { this.ministere = ministere; }
    public Boolean getActif() { return actif; }
    public void setActif(Boolean actif) { this.actif = actif; }
    public Boolean getAlertesActivees() { return alertesActivees; }
    public void setAlertesActivees(Boolean alertesActivees) { this.alertesActivees = alertesActivees; }
    public Instant getCreeLe() { return creeLe; }
    public void setCreeLe(Instant creeLe) { this.creeLe = creeLe; }
    public String getCreePar() { return creePar; }
    public void setCreePar(String creePar) { this.creePar = creePar; }
}