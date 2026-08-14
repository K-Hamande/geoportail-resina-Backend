package bf.anptic.geoportail.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "ministry_access_tokens", schema = "geoportail_resina")
public class MinistryAccessToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String token;
    private String ministere;
    private String libelle;
    private Boolean actif;
    private Instant creeLe;
    private String creePar;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getMinistere() { return ministere; }
    public void setMinistere(String ministere) { this.ministere = ministere; }
    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }
    public Boolean getActif() { return actif; }
    public void setActif(Boolean actif) { this.actif = actif; }
    public Instant getCreeLe() { return creeLe; }
    public void setCreeLe(Instant creeLe) { this.creeLe = creeLe; }
    public String getCreePar() { return creePar; }
    public void setCreePar(String creePar) { this.creePar = creePar; }
}