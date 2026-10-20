package bf.anptic.geoportail.model;

import jakarta.persistence.*;

import java.time.Instant;

// Copie locale persistante des tickets iTop (classe Ticket/Incident cote
// iTop) - alimentee par synchronisation periodique (cf.
// ItopTicketHistoryService + ItopSyncScheduler), PAS calculee a la volee :
// contrairement aux alertes reseau (IncidentHistorique), iTop porte deja
// lui-meme ses propres dates de debut/cloture - on se contente de les
// recopier ici pour un affichage/filtrage rapide cote Backoffice, sans
// dependre de la disponibilite d'iTop a chaque consultation de la page.
@Entity
@Table(name = "itop_ticket_historique", schema = "geoportail_resina")
public class ItopTicketHistorique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Reference iTop (ex: "I-001811") - cle metier stable, unique, sert a
    // retrouver/mettre a jour l'enregistrement correspondant a chaque sync.
    @Column(nullable = false, unique = true)
    private String ref;

    private String titre;

    private String ministere;

    // Valeur brute d'operational_status cote iTop : "ongoing", "resolved"
    // ou "closed".
    private String statut;

    @Column(nullable = false)
    private Instant survenuLe;

    // null tant que le ticket n'est pas cloture cote iTop.
    private Instant clotureLe;

    // last_update iTop au moment de la derniere synchronisation - sert de
    // filtre incremental (ne resynchronise que ce qui a change depuis le
    // dernier passage, cf. ItopClient.listerTicketsModifiesDepuis).
    private Instant derniereMajItop;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRef() { return ref; }
    public void setRef(String ref) { this.ref = ref; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getMinistere() { return ministere; }
    public void setMinistere(String ministere) { this.ministere = ministere; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public Instant getSurvenuLe() { return survenuLe; }
    public void setSurvenuLe(Instant survenuLe) { this.survenuLe = survenuLe; }

    public Instant getClotureLe() { return clotureLe; }
    public void setClotureLe(Instant clotureLe) { this.clotureLe = clotureLe; }

    public Instant getDerniereMajItop() { return derniereMajItop; }
    public void setDerniereMajItop(Instant derniereMajItop) { this.derniereMajItop = derniereMajItop; }
}
