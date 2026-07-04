package bf.anptic.geoportail.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
// @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    // §6.2 du CDC : identifiant du noeud correspondant dans NetXMS
    private Integer netxmsNodeId;

    // §6.2 du CDC : nombre d'etages surveilles
    private Integer niveaux;

    // §3.2.6b : un site peut etre desactive sans etre supprime
    private Boolean actif;

    private String infoAuSurvol;

    // ---- Getters et setters ----

    public String getSiteId() {
        return siteId;
    }

    public void setSiteId(String siteId) {
        this.siteId = siteId;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getVille() {
        return ville;
    }

    public void setVille(String ville) {
        this.ville = ville;
    }

    public String getRegionAdministrative() {
        return regionAdministrative;
    }

    public void setRegionAdministrative(String regionAdministrative) {
        this.regionAdministrative = regionAdministrative;
    }

    public String getBatiment() {
        return batiment;
    }

    public void setBatiment(String batiment) {
        this.batiment = batiment;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getContactDsiNom() {
        return contactDsiNom;
    }

    public void setContactDsiNom(String contactDsiNom) {
        this.contactDsiNom = contactDsiNom;
    }

    public String getContactDsiTelephone() {
        return contactDsiTelephone;
    }

    public void setContactDsiTelephone(String contactDsiTelephone) {
        this.contactDsiTelephone = contactDsiTelephone;
    }

    public Integer getNetxmsNodeId() {
        return netxmsNodeId;
    }

    public void setNetxmsNodeId(Integer netxmsNodeId) {
        this.netxmsNodeId = netxmsNodeId;
    }

    public Integer getNiveaux() {
        return niveaux;
    }

    public void setNiveaux(Integer niveaux) {
        this.niveaux = niveaux;
    }

    public Boolean getActif() {
        return actif;
    }

    public void setActif(Boolean actif) {
        this.actif = actif;
    }


    public String getInfoAuSurvol() {
        return infoAuSurvol;
    }

    public void setInfoAuSurvol(String infoAuSurvol) {
        this.infoAuSurvol = infoAuSurvol;
    }


}