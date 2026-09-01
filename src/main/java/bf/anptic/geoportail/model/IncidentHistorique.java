package bf.anptic.geoportail.model;

import jakarta.persistence.*;

import java.time.Instant;

// Trace persistante d'un incident (panne ANPTIC ou LAN) sur un site,
// depuis sa detection jusqu'a sa resolution. Contrairement au calcul a la
// volee de IncidentService (qui ne reflete que l'etat ACTUEL du reseau),
// cette table garde un historique date : finLe == null signifie que
// l'incident est toujours en cours. Alimentee par IncidentHistoryService,
// qui compare periodiquement l'etat en temps reel a cette table pour
// ouvrir/mettre a jour/fermer chaque enregistrement.
@Entity
@Table(name = "incident_historique", schema = "geoportail_resina")
public class IncidentHistorique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Cle stable de l'incident tant qu'il reste actif : "{siteId}-anptic"
    // ou "{siteId}-lan" (meme format que IncidentDto.id()). Sert a
    // retrouver l'enregistrement OUVERT correspondant a un incident actif.
    @Column(nullable = false)
    private String incidentKey;

    private String type; // "ANPTIC" ou "LAN"

    private String siteId;

    private String siteNom;

    private String ville;

    private String ministere; // peut etre null

    // Dernier statut connu (NodeStatus.name(), ex: "WARN" ou "KO") - peut
    // s'aggraver pendant que l'incident reste ouvert (WARN -> KO).
    private String statut;

    @Column(length = 1000)
    private String message;

    @Column(nullable = false)
    private Instant debutLe;

    // null tant que l'incident est en cours.
    private Instant finLe;

    // ---- Getters et setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIncidentKey() { return incidentKey; }
    public void setIncidentKey(String incidentKey) { this.incidentKey = incidentKey; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSiteId() { return siteId; }
    public void setSiteId(String siteId) { this.siteId = siteId; }

    public String getSiteNom() { return siteNom; }
    public void setSiteNom(String siteNom) { this.siteNom = siteNom; }

    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }

    public String getMinistere() { return ministere; }
    public void setMinistere(String ministere) { this.ministere = ministere; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Instant getDebutLe() { return debutLe; }
    public void setDebutLe(Instant debutLe) { this.debutLe = debutLe; }

    public Instant getFinLe() { return finLe; }
    public void setFinLe(Instant finLe) { this.finLe = finLe; }
}