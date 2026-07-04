package bf.anptic.geoportail.client;

import bf.anptic.geoportail.config.NetXmsProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;

@Component
public class NetXmsClient {

    private final RestClient restClient;

    public NetXmsClient(NetXmsProperties properties) {
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getBearerToken())
                .build();
    }

    public NodeMetrics getNodeMetrics(int nodeId) {
        JsonNode node = restClient.get()
                .uri("/objects/{id}", nodeId)
                .retrieve()
                .body(JsonNode.class);

        NodeMetrics metrics = new NodeMetrics();
        metrics.setStatus(textOrNull(node, "status"));
        metrics.setInboundMbps(doubleOrNull(node, "inboundUtilizationMbps"));
        metrics.setOutboundMbps(doubleOrNull(node, "outboundUtilizationMbps"));
        metrics.setLatencyMs(doubleOrNull(node, "pingLastValueMs"));
        metrics.setAvailability30d(doubleOrNull(node, "availability30d"));
        metrics.setLinkType(textOrNull(node, "linkType"));
        metrics.setSignalQuality(textOrNull(node, "signalQuality"));

        String lastStatusChange = textOrNull(node, "lastStatusChange");
        if (lastStatusChange != null) {
            metrics.setDownSince(Instant.parse(lastStatusChange));
        }

        return metrics;
    }

    // Petites methodes utilitaires : le JSON peut contenir des champs
    // "null" (comme dans notre mock pour le cas panne), il faut donc
    // verifier avant d'appeler .asText() / .asDouble() pour ne pas
    // planter avec une NullPointerException.
    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return (value == null || value.isNull()) ? null : value.asText();
    }

    private static Double doubleOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return (value == null || value.isNull()) ? null : value.asDouble();
    }


    // Recupere le statut de chaque equipement LAN (objet enfant)
    // rattache a un noeud site.
    public java.util.Map<Integer, String> getChildrenStatuses(int parentNodeId) {
        JsonNode children = restClient.get()
                .uri("/objects/{id}/children", parentNodeId)
                .retrieve()
                .body(JsonNode.class);

        java.util.Map<Integer, String> statusByObjectId = new java.util.HashMap<>();
        if (children != null && children.isArray()) {
            for (JsonNode child : children) {
                int objectId = child.get("id").asInt();
                String status = child.get("status").asText();
                statusByObjectId.put(objectId, status);
            }
        }
        return statusByObjectId;
    }


}