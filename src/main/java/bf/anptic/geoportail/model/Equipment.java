package bf.anptic.geoportail.model;

import jakarta.persistence.*;

// Represente un equipement LAN (borne Wi-Fi ou commutateur) rattache
// a un etage d'un site (§3.2.6a du CDC).
@Entity
@Table(name = "equipments", schema = "geoportail_resina")
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "site_id")
    private Site site;

    private String etageLabel;      // ex: "RDC", "R+1", "R+2"

    @Enumerated(EnumType.STRING)
    private EquipmentType type;

    // Libelle presente cote DECIDEUR. Volontairement DISTINCT du nom
    // technique NetXMS (voir nomTechniqueNetxms) : le CDC (§4.4) interdit
    // d'exposer des noms d'equipement internes au client. Reste null tant
    // qu'un admin ne l'a pas personnalise - un libelle generique est
    // genere a l'affichage dans ce cas (voir LanStatusService).
    private String libelleAffiche;

    // Nom technique brut tel que remonte par NetXMS (ex: "FADA-SHELTER-
    // BSR180") - reference INTERNE pour l'admin backoffice uniquement,
    // jamais transmis au frontend decideur.
    private String nomTechniqueNetxms;

    private Integer netxmsObjectId;

    // Proprietaire de l'equipement tel que remonte par NetXMS
    // (geo_equipement.propriete) : 'ANPTIC' ou l'acronyme d'un ministere
    // (ex: 'MEFP'). Conserve pour un usage futur eventuel (badge
    // ANPTIC/ministere cote decideur ou Backoffice) - aucune lecture ne
    // filtre dessus actuellement, le decideur voit tous les equipements.
    private String propriete;

    public enum EquipmentType {
        BORNE_WIFI,     // Point d'acces Wi-Fi
        COMMUTATEUR,    // Switch
        ROUTEUR,        // Router - liaison WAN
        PTP,            // Point-to-Point (faisceau hertzien)
        PMP,            // Point-to-Multipoint
        CPE,            // Customer Premises Equipment (terminaison operateur)
        ONDULEUR,       // Alimentation electrique
        SERVEUR,        // Serveur physique
        PYLONE,         // Infrastructure de support (pylone/tour)
        AUTRE           // Type non reconnu - fallback
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

    public String getNomTechniqueNetxms() {
        return nomTechniqueNetxms;
    }

    public void setNomTechniqueNetxms(String nomTechniqueNetxms) {
        this.nomTechniqueNetxms = nomTechniqueNetxms;
    }

    public Integer getNetxmsObjectId() {
        return netxmsObjectId;
    }

    public void setNetxmsObjectId(Integer netxmsObjectId) {
        this.netxmsObjectId = netxmsObjectId;
    }

    public String getPropriete() {
        return propriete;
    }

    public void setPropriete(String propriete) {
        this.propriete = propriete;
    }
}