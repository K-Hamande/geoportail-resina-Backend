package bf.anptic.geoportail.model;

import jakarta.persistence.*;

// Represente un equipement LAN (borne Wi-Fi ou commutateur) rattache
// a un etage d'un site (§3.2.6a du CDC).
@Entity
@Table(name = "equipments")
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // l'id est auto-genere par la base (1, 2, 3...)
    private Long id;

    // @ManyToOne : PLUSIEURS Equipment peuvent pointer vers UN Site.
    // @JoinColumn precise le nom de la colonne de cle etrangere en base.
    @ManyToOne
    @JoinColumn(name = "site_id")
    private Site site;

    private String etageLabel;      // ex: "RDC", "R+1", "R+2"

    @Enumerated(EnumType.STRING)    // stocke le nom de l'enum en texte ("BORNE_WIFI"), pas un simple numero
    private EquipmentType type;

    private String libelleAffiche;  // libelle presente cote decideur

    private Integer netxmsObjectId; // id de l'objet correspondant dans NetXMS

    public enum EquipmentType {
        BORNE_WIFI,
        COMMUTATEUR
    }

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

    public String getEtageLabel() {
        return etageLabel;
    }

    public void setEtageLabel(String etageLabel) {
        this.etageLabel = etageLabel;
    }

    public EquipmentType getType() {
        return type;
    }

    public void setType(EquipmentType type) {
        this.type = type;
    }

    public String getLibelleAffiche() {
        return libelleAffiche;
    }

    public void setLibelleAffiche(String libelleAffiche) {
        this.libelleAffiche = libelleAffiche;
    }

    public Integer getNetxmsObjectId() {
        return netxmsObjectId;
    }

    public void setNetxmsObjectId(Integer netxmsObjectId) {
        this.netxmsObjectId = netxmsObjectId;
    }
}