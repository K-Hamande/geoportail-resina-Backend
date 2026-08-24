package bf.anptic.geoportail.model;

import jakarta.persistence.*;

import java.time.Instant;

// Destinataire enregistre pour les notifications push d'un site
// (§3.2.6b : "enregistrement et suppression des tokens de notification
// par site et par profil utilisateur").
@Entity
@Table(name = "notification_tokens", schema = "geoportail_resina")
public class NotificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "site_id")
    private Site site;

    private String profil;       // ex: "Ministre", "Protocole"

    private String plateforme;   // "ANDROID" ou "IOS"

    @Column(unique = true)
    private String token;        // identifiant fourni par le systeme de notification du telephone

    private Boolean actif;

    private Instant enregistreLe;

    // ---- Getters et setters ----

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Site getSite() {
        return site;
    }

    public void setSite(Site site) {
        this.site = site;
    }

    public String getProfil() {
        return profil;
    }

    public void setProfil(String profil) {
        this.profil = profil;
    }

    public String getPlateforme() {
        return plateforme;
    }

    public void setPlateforme(String plateforme) {
        this.plateforme = plateforme;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Boolean getActif() {
        return actif;
    }

    public void setActif(Boolean actif) {
        this.actif = actif;
    }

    public Instant getEnregistreLe() {
        return enregistreLe;
    }

    public void setEnregistreLe(Instant enregistreLe) {
        this.enregistreLe = enregistreLe;
    }
}