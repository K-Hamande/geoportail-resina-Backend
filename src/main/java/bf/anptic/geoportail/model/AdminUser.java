package bf.anptic.geoportail.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "admin_users")
public class AdminUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String login;

    private String nomComplet;

    @Column(nullable = false)
    private String motDePasseHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // Vide = acces global (tous les sites). Non vide = restreint a ces
    // site_id precis (§3.2.6b : "attribution de droits par site ou globaux").
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "admin_user_sites", joinColumns = @JoinColumn(name = "admin_user_id"))
    @Column(name = "site_id")
    private Set<String> sitesAutorises = new HashSet<>();

    private Boolean actif;

    private Instant creeLe;

    public enum Role {
        SUPER_ADMIN,
        ADMIN_DEST,
        ADMIN_DIG
    }

    @PrePersist
    void onCreate() {
        this.creeLe = Instant.now();
    }

    // ---- Getters et setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getNomComplet() { return nomComplet; }
    public void setNomComplet(String nomComplet) { this.nomComplet = nomComplet; }

    public String getMotDePasseHash() { return motDePasseHash; }
    public void setMotDePasseHash(String motDePasseHash) { this.motDePasseHash = motDePasseHash; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public Set<String> getSitesAutorises() { return sitesAutorises; }
    public void setSitesAutorises(Set<String> sitesAutorises) { this.sitesAutorises = sitesAutorises; }

    public Boolean getActif() { return actif; }
    public void setActif(Boolean actif) { this.actif = actif; }

    public Instant getCreeLe() { return creeLe; }
    public void setCreeLe(Instant creeLe) { this.creeLe = creeLe; }

    /** true si ce compte a acces a TOUS les sites (role SUPER_ADMIN, ou aucune restriction definie). */
    public boolean aAccesGlobal() {
        return role == Role.SUPER_ADMIN || sitesAutorises.isEmpty();
    }

    public boolean peutAccederAuSite(String siteId) {
        return aAccesGlobal() || sitesAutorises.contains(siteId);
    }
}