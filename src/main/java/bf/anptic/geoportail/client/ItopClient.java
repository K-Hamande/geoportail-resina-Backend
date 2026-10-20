package bf.anptic.geoportail.client;

import bf.anptic.geoportail.config.ItopProperties;
import bf.anptic.geoportail.dto.TicketIncidentDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Client de l'API REST d'iTop (outil ITSM de l'ANPTIC) - cf. ItopProperties.
// Contrairement a NetXmsClient (API REST "moderne", un token Bearer, un
// chemin par ressource), l'API REST d'iTop est plus ancienne : un seul
// endpoint, toujours en POST, avec un corps x-www-form-urlencoded portant
// 3 champs (auth_user / auth_pwd / json_data) - json_data etant lui-meme
// une chaine JSON decrivant l'operation ("core/get", classe, requete OQL,
// champs voulus).
@Component
public class ItopClient {

    private static final Logger log = LoggerFactory.getLogger(ItopClient.class);
    private static final ZoneId FUSEAU = ZoneId.of("Africa/Ouagadougou");
    private static final DateTimeFormatter FORMAT_DATE_ITOP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ItopProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public ItopClient(ItopProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
    }

    // Tickets d'incident ouverts (non clotures) pour UN ministere donne.
    // Le filtrage par ministere est fait cote iTop (clause OQL), pas en
    // Java : un decideur ne doit jamais recevoir, meme transitoirement en
    // memoire, les tickets d'un autre ministere.
    public List<TicketIncidentDto> listerTicketsOuverts(String ministere) {
        String oql = "SELECT Ticket WHERE org_name = \"" + ministere + "\" AND operational_status != \"closed\"";
        JsonNode reponse = appeler(oql, CHAMPS_SANS_DERNIERE_MAJ, null);
        return extraireTickets(reponse);
    }

    // Tous les tickets (tous ministeres confondus, ouverts ET clotures) -
    // pour la synchronisation de l'historique Backoffice (cf.
    // ItopTicketHistoryService), PAS pour un decideur (aucun filtre
    // ministere ici, volontairement). "modifiesDepuis" permet une
    // synchronisation incrementale (ne redemande que ce qui a change
    // depuis le dernier passage) ; null => synchronisation complete,
    // bornee par "limite" par securite (eviter un payload sans fin si la
    // base de tickets grossit beaucoup).
    public List<TicketBrut> listerTousLesTickets(Instant modifiesDepuis, int limite) {
        String oql = modifiesDepuis == null
                ? "SELECT Ticket"
                : "SELECT Ticket WHERE last_update >= \"" + formaterDateItop(modifiesDepuis) + "\"";
        JsonNode reponse = appeler(oql, CHAMPS_AVEC_DERNIERE_MAJ, limite);
        return extraireTicketsBruts(reponse);
    }

    private static final String CHAMPS_SANS_DERNIERE_MAJ = "ref,title,org_name,operational_status,start_date,close_date";
    private static final String CHAMPS_AVEC_DERNIERE_MAJ = CHAMPS_SANS_DERNIERE_MAJ + ",last_update";

    private JsonNode appeler(String oql, String champs, Integer limite) {
        String jsonData = construireJsonData(oql, champs, limite);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("auth_user", properties.getLogin());
        form.add("auth_pwd", properties.getPassword());
        form.add("json_data", jsonData);

        return restClient.post()
                .uri(properties.getBaseUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(JsonNode.class);
    }

    private String construireJsonData(String oql, String champs, Integer limite) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("operation", "core/get");
        payload.put("class", "Ticket");
        payload.put("key", oql);
        payload.put("output_fields", champs);
        if (limite != null) payload.put("limit", limite);
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de construire la requete iTop", e);
        }
    }

    private static String formaterDateItop(Instant instant) {
        return FORMAT_DATE_ITOP.format(LocalDateTime.ofInstant(instant, FUSEAU));
    }

    // Variante "brute" de TicketIncidentDto, avec en plus last_update
    // (necessaire pour la synchronisation incrementale) - distincte du DTO
    // partage avec le flux decideur temps reel pour ne pas alourdir son
    // contrat avec un champ qui ne lui sert pas.
    public record TicketBrut(String ref, String titre, String ministere, String statut,
                              Instant survenuLe, Instant clotureLe, Instant derniereMaj) {}

    private List<TicketIncidentDto> extraireTickets(JsonNode reponse) {
        List<TicketIncidentDto> tickets = new ArrayList<>();
        if (reponse == null) return tickets;

        int code = reponse.path("code").asInt(-1);
        if (code != 0) {
            log.warn("Reponse iTop en erreur (code={}) : {}", code, reponse.path("message").asText());
            return tickets;
        }

        JsonNode objets = reponse.path("objects");
        Iterator<String> cles = objets.fieldNames();
        while (cles.hasNext()) {
            JsonNode champs = objets.get(cles.next()).path("fields");
            tickets.add(new TicketIncidentDto(
                    texteOuNull(champs, "ref"),
                    texteOuNull(champs, "title"),
                    texteOuNull(champs, "org_name"),
                    texteOuNull(champs, "operational_status"),
                    dateOuNull(champs, "start_date"),
                    dateOuNull(champs, "close_date")
            ));
        }
        return tickets;
    }

    private List<TicketBrut> extraireTicketsBruts(JsonNode reponse) {
        List<TicketBrut> tickets = new ArrayList<>();
        if (reponse == null) return tickets;

        int code = reponse.path("code").asInt(-1);
        if (code != 0) {
            log.warn("Reponse iTop en erreur (code={}) : {}", code, reponse.path("message").asText());
            return tickets;
        }

        JsonNode objets = reponse.path("objects");
        Iterator<String> cles = objets.fieldNames();
        while (cles.hasNext()) {
            JsonNode champs = objets.get(cles.next()).path("fields");
            tickets.add(new TicketBrut(
                    texteOuNull(champs, "ref"),
                    texteOuNull(champs, "title"),
                    texteOuNull(champs, "org_name"),
                    texteOuNull(champs, "operational_status"),
                    dateOuNull(champs, "start_date"),
                    dateOuNull(champs, "close_date"),
                    dateOuNull(champs, "last_update")
            ));
        }
        return tickets;
    }

    private static String texteOuNull(JsonNode node, String champ) {
        JsonNode valeur = node.get(champ);
        if (valeur == null || valeur.isNull() || valeur.asText().isBlank()) return null;
        return valeur.asText();
    }

    private static Instant dateOuNull(JsonNode node, String champ) {
        String texte = texteOuNull(node, champ);
        if (texte == null) return null;
        return LocalDateTime.parse(texte, FORMAT_DATE_ITOP).atZone(FUSEAU).toInstant();
    }
}
