package bf.anptic.geoportail.client;

import java.time.Instant;

public class NodeMetrics {

    private String status;
    private Double inboundMbps;
    private Double outboundMbps;
    private Double latencyMs;
    private Double availability30d;
    private String linkType;
    private String signalQuality;
    private Instant downSince;   // rempli uniquement si la liaison est en panne

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getInboundMbps() {
        return inboundMbps;
    }

    public void setInboundMbps(Double inboundMbps) {
        this.inboundMbps = inboundMbps;
    }

    public Double getOutboundMbps() {
        return outboundMbps;
    }

    public void setOutboundMbps(Double outboundMbps) {
        this.outboundMbps = outboundMbps;
    }

    public Double getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Double latencyMs) {
        this.latencyMs = latencyMs;
    }

    public Double getAvailability30d() {
        return availability30d;
    }

    public void setAvailability30d(Double availability30d) {
        this.availability30d = availability30d;
    }

    public String getLinkType() {
        return linkType;
    }

    public void setLinkType(String linkType) {
        this.linkType = linkType;
    }

    public String getSignalQuality() {
        return signalQuality;
    }

    public void setSignalQuality(String signalQuality) {
        this.signalQuality = signalQuality;
    }

    public Instant getDownSince() {
        return downSince;
    }

    public void setDownSince(Instant downSince) {
        this.downSince = downSince;
    }
}