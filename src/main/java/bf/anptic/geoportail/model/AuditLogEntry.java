package bf.anptic.geoportail.model;

import jakarta.persistence.*;

import java.time.Instant;

// Trace une action effectuee dans le Backoffice : qui, quoi, quand
// (§3.2.6b du CDC : "tracabilite des modifications effectuees").
@Entity
@Table(name = "audit_log_entries", schema = "geoportail_resina")
public class AuditLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String auteur;    // le login de l'admin qui a fait l'action

    private String action;    // ex: "Creation site", "Suppression equipement"

    @Column(length = 1000)
    private String details;   // ex: "site=primature"

    private Instant horodatage;

    // ---- Getters et setters ----

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAuteur() {
        return auteur;
    }

    public void setAuteur(String auteur) {
        this.auteur = auteur;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Instant getHorodatage() {
        return horodatage;
    }

    public void setHorodatage(Instant horodatage) {
        this.horodatage = horodatage;
    }
}