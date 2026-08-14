package bf.anptic.geoportail.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sites", schema = "geoportail_resina")
public class Site {

    @Id
    private String siteId;

    private String nom;
    private String ville;
    private String regionAdministrative;
    private String batiment;
    private Double latitude;
    private Double longitude;
    private String contactDsiNom;
    private String contactDsiTelephone;
    private String contactDsiEmail;
    private Integer netxmsNodeId;
    private Integer niveaux;
    private Boolean actif;
    private String infoAuSurvol;

    // Utilise pour restreindre l'acces decideur par lien securise
    // (§4.4 du CDC) : un lien = un ministere = les sites qui lui
    // appartiennent uniquement.
    private String ministere;

    public String getSiteId() { return siteId; }
    public void setSiteId(String siteId) { this.siteId = siteId; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }
    public String getRegionAdministrative() { return regionAdministrative; }
    public void setRegionAdministrative(String regionAdministrative) { this.regionAdministrative = regionAdministrative; }
    public String getBatiment() { return batiment; }
    public void setBatiment(String batiment) { this.batiment = batiment; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getContactDsiNom() { return contactDsiNom; }
    public void setContactDsiNom(String contactDsiNom) { this.contactDsiNom = contactDsiNom; }
    public String getContactDsiTelephone() { return contactDsiTelephone; }
    public void setContactDsiTelephone(String contactDsiTelephone) { this.contactDsiTelephone = contactDsiTelephone; }
    public String getContactDsiEmail() { return contactDsiEmail; }
    public void setContactDsiEmail(String contactDsiEmail) { this.contactDsiEmail = contactDsiEmail; }
    public Integer getNetxmsNodeId() { return netxmsNodeId; }
    public void setNetxmsNodeId(Integer netxmsNodeId) { this.netxmsNodeId = netxmsNodeId; }
    public Integer getNiveaux() { return niveaux; }
    public void setNiveaux(Integer niveaux) { this.niveaux = niveaux; }
    public Boolean getActif() { return actif; }
    public void setActif(Boolean actif) { this.actif = actif; }
    public String getInfoAuSurvol() { return infoAuSurvol; }
    public void setInfoAuSurvol(String infoAuSurvol) { this.infoAuSurvol = infoAuSurvol; }
    public String getMinistere() { return ministere; }
    public void setMinistere(String ministere) { this.ministere = ministere; }
}