package bf.anptic.geoportail.model;

import jakarta.persistence.*;
import java.time.Instant;
import jakarta.persistence.Column;

// Parametres de supervision propres a UN site (§3.2.6b du CDC) :
// intervalle d'actualisation, seuils d'alerte, activation des
// notifications push par type d'evenement.
// Un site sans ligne ici utilise les valeurs par defaut (voir
// SupervisionSettingsService.DEFAUTS).
@Entity
@Table(name = "site_supervision_settings", schema = "geoportail_resina")
public class SiteSupervisionSettings {

    @Id
    @Column(name = "site_id")
    private String siteId;

    @Column(name = "intervalle_actualisation_s")
    private Integer intervalleActualisationS;    
    private Double debitMinimalMbps;
    private Double latenceMaximaleMs;
    private Boolean notificationsActives;
    private Boolean notifPanneAnptic;
    private Boolean notifPanneLan;
    private Boolean notifRetablissement;
    private Instant modifieLe;
    private String modifiePar;

    public String getSiteId() { return siteId; }
    public void setSiteId(String siteId) { this.siteId = siteId; }
    public Integer getIntervalleActualisationS() { return intervalleActualisationS; }
    public void setIntervalleActualisationS(Integer v) { this.intervalleActualisationS = v; }
    public Double getDebitMinimalMbps() { return debitMinimalMbps; }
    public void setDebitMinimalMbps(Double v) { this.debitMinimalMbps = v; }
    public Double getLatenceMaximaleMs() { return latenceMaximaleMs; }
    public void setLatenceMaximaleMs(Double v) { this.latenceMaximaleMs = v; }
    public Boolean getNotificationsActives() { return notificationsActives; }
    public void setNotificationsActives(Boolean v) { this.notificationsActives = v; }
    public Boolean getNotifPanneAnptic() { return notifPanneAnptic; }
    public void setNotifPanneAnptic(Boolean v) { this.notifPanneAnptic = v; }
    public Boolean getNotifPanneLan() { return notifPanneLan; }
    public void setNotifPanneLan(Boolean v) { this.notifPanneLan = v; }
    public Boolean getNotifRetablissement() { return notifRetablissement; }
    public void setNotifRetablissement(Boolean v) { this.notifRetablissement = v; }
    public Instant getModifieLe() { return modifieLe; }
    public void setModifieLe(Instant v) { this.modifieLe = v; }
    public String getModifiePar() { return modifiePar; }
    public void setModifiePar(String v) { this.modifiePar = v; }
}