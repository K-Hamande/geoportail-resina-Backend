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
    private String province;
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
    private String ministere;
    private String structure;

    public String getSiteId() { return siteId; }
    public void setSiteId(String siteId) { this.siteId = siteId; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }
    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
    public String getRegionAdministrative() { return regionAdministrative; }
    public void setRegionAdministrative(String r) { this.regionAdministrative = r; }
    public String getBatiment() { return batiment; }
    public void setBatiment(String batiment) { this.batiment = batiment; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getContactDsiNom() { return contactDsiNom; }
    public void setContactDsiNom(String v) { this.contactDsiNom = v; }
    public String getContactDsiTelephone() { return contactDsiTelephone; }
    public void setContactDsiTelephone(String v) { this.contactDsiTelephone = v; }
    public String getContactDsiEmail() { return contactDsiEmail; }
    public void setContactDsiEmail(String v) { this.contactDsiEmail = v; }
    public Integer getNetxmsNodeId() { return netxmsNodeId; }
    public void setNetxmsNodeId(Integer v) { this.netxmsNodeId = v; }
    public Integer getNiveaux() { return niveaux; }
    public void setNiveaux(Integer niveaux) { this.niveaux = niveaux; }
    public Boolean getActif() { return actif; }
    public void setActif(Boolean actif) { this.actif = actif; }
    public String getInfoAuSurvol() { return infoAuSurvol; }
    public void setInfoAuSurvol(String v) { this.infoAuSurvol = v; }
    public String getMinistere() { return ministere; }
    public void setMinistere(String ministere) { this.ministere = ministere; }
    public String getStructure() { return structure; }
    public void setStructure(String structure) { this.structure = structure; }
}