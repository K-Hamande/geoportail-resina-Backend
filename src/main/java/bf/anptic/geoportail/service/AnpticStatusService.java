package bf.anptic.geoportail.service;

import bf.anptic.geoportail.dto.AnpticStatusDto;
import bf.anptic.geoportail.model.Site;
import bf.anptic.geoportail.model.enums.NodeStatus;
import bf.anptic.geoportail.repository.SiteRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

// Lit le statut de la liaison ANPTIC (WAN) d'un site directement depuis
// netxmsdb : equipement "Routeur" du site (geo_equipement), latence
// (items/raw_dci_values) et disponibilite sur 30 jours (nodes +
// object_properties + downtime_log).
@Service
public class AnpticStatusService {

    private static final String SELECT_ROUTEUR = """
            SELECT object_id, status, technologie, traffic_entrant, traffic_sortant
            FROM public.geo_equipement
            WHERE siteadmin_id = ? AND type ILIKE '%rout%'
            ORDER BY object_id
            LIMIT 1
            """;

    private static final String SELECT_PREMIER_EQUIPEMENT = """
            SELECT object_id, status, technologie, traffic_entrant, traffic_sortant
            FROM public.geo_equipement
            WHERE siteadmin_id = ?
            ORDER BY object_id
            LIMIT 1
            """;

    private static final String SELECT_LATENCE = """
            SELECT rdci.transformed_value
            FROM public.items dct
            JOIN public.raw_dci_values rdci ON dct.item_id = rdci.item_id
            WHERE dct.node_id = ? AND dct.description ILIKE '%last response time%'
            LIMIT 1
            """;

    private static final String SELECT_DISPONIBILITE_30J = """
            SELECT n.down_since,
                   round(COALESCE(
                       CASE
                           WHEN dl.object_id IS NULL AND n.down_since <> 0 OR op.status = 6 THEN 0::numeric
                           WHEN dl.object_id IS NULL AND n.down_since = 0 THEN 100::numeric
                           ELSE 100.0 - sum(
                               CASE
                                   WHEN dl.end_time = 0 AND dl.start_time::numeric < EXTRACT(epoch FROM now() - interval '30 days')
                                       THEN EXTRACT(epoch FROM now()) - EXTRACT(epoch FROM now() - interval '30 days')
                                   WHEN dl.end_time = 0
                                       THEN EXTRACT(epoch FROM now()) - dl.start_time::numeric
                                   WHEN dl.start_time::numeric < EXTRACT(epoch FROM now() - interval '30 days')
                                       THEN dl.end_time::numeric - EXTRACT(epoch FROM now() - interval '30 days')
                                   ELSE (dl.end_time - dl.start_time)::numeric
                               END) * 100.0 / (EXTRACT(epoch FROM now()) - EXTRACT(epoch FROM now() - interval '30 days'))
                       END, 100.0), 1) AS pourcentage_30j
            FROM public.object_properties op
            JOIN public.nodes n ON op.object_id = n.id
            LEFT JOIN public.downtime_log dl ON dl.object_id = op.object_id
                AND (dl.start_time::numeric >= EXTRACT(epoch FROM now() - interval '30 days') OR dl.end_time = 0)
            WHERE op.object_id = ?
            GROUP BY op.object_id, n.down_since, op.status, dl.object_id
            """;

    private final SiteRepository siteRepository;
    private final JdbcTemplate netxmsJdbcTemplate;

    public AnpticStatusService(SiteRepository siteRepository,
                                @Qualifier("netxmsJdbcTemplate") JdbcTemplate netxmsJdbcTemplate) {
        this.siteRepository = siteRepository;
        this.netxmsJdbcTemplate = netxmsJdbcTemplate;
    }

    public AnpticStatusDto getAnpticStatus(String siteId) {
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Site introuvable : " + siteId));

        if (site.getNetxmsNodeId() == null) {
            return new AnpticStatusDto(siteId, NodeStatus.UNKNOWN, false,
                    "Ce site n'est pas encore relie a NetXMS", null, null, null, null, null, null, null,
                    "Contactez l'ANPTIC pour finaliser le rattachement de ce site");
        }
        int siteAdminId = site.getNetxmsNodeId();

        List<EquipementRow> routeurs = netxmsJdbcTemplate.query(SELECT_ROUTEUR, EquipementRow::of, siteAdminId);
        EquipementRow equipement = routeurs.stream().findFirst().orElseGet(() ->
                netxmsJdbcTemplate.query(SELECT_PREMIER_EQUIPEMENT, EquipementRow::of, siteAdminId)
                        .stream().findFirst().orElse(null));

        if (equipement == null) {
            return new AnpticStatusDto(siteId, NodeStatus.UNKNOWN, false,
                    "Aucun equipement reseau recense pour ce site", null, null, null, null, null, null, null,
                    "Contactez l'ANPTIC pour verifier le rattachement de ce site");
        }

        Double debitMontant = parseNombre(equipement.trafficSortant());
        Double debitDescendant = parseNombre(equipement.trafficEntrant());

        // Choix produit (validé) : si du trafic reel est mesure sur cet
        // equipement, on considere la liaison comme disponible et on
        // affiche le debit - meme si le statut de supervision NetXMS
        // brut n'est pas "Normal" (ex: equipement marque "Non gere" ou
        // "Critique" alors que des donnees continuent de transiter,
        // frequent quand la supervision active n'est pas configuree
        // mais que le lien physique fonctionne). Sans cet assouplissement,
        // la quasi-totalite des sites du reseau actuel n'afficherait
        // jamais de debit, le statut NetXMS "Normal" (0) etant tres rare
        // dans ce jeu de donnees.
        boolean trafficReel = (debitMontant != null && debitMontant > 0) || (debitDescendant != null && debitDescendant > 0);
        NodeStatus statutCalcule = NodeStatus.fromNetXmsSeverityCode(equipement.status());
        NodeStatus status = trafficReel ? NodeStatus.OK : statutCalcule;
        boolean disponible = status == NodeStatus.OK;

        Double latenceMs = netxmsJdbcTemplate.query(SELECT_LATENCE,
                        (rs, rowNum) -> rs.getString(1), equipement.objectId())
                .stream().findFirst()
                .map(AnpticStatusService::parseNombre)
                .orElse(null);

        DisponibiliteRow dispo = netxmsJdbcTemplate.query(SELECT_DISPONIBILITE_30J,
                        (rs, rowNum) -> {
                            long downSince = rs.getLong("down_since");
                            double pourcentage = rs.getDouble("pourcentage_30j");
                            return new DisponibiliteRow(downSince, pourcentage);
                        }, equipement.objectId())
                .stream().findFirst().orElse(new DisponibiliteRow(0L, null));

        if (disponible) {
            return new AnpticStatusDto(
                    siteId,
                    status,
                    true,
                    "La connexion ANPTIC est disponible",
                    debitMontant,
                    debitDescendant,
                    equipement.technologie(),
                    null,
                    latenceMs,
                    dispo.pourcentage(),
                    null,
                    null
            );
        } else {
            Instant indisponibleDepuis = dispo.downSince() != 0
                    ? Instant.ofEpochSecond(dispo.downSince())
                    : null;
            return new AnpticStatusDto(
                    siteId,
                    status,
                    false,
                    "La connexion ANPTIC n'est pas disponible",
                    null,
                    null,
                    equipement.technologie(),
                    null,
                    null,
                    dispo.pourcentage(),
                    indisponibleDepuis,
                    "Veuillez contacter les services support de l'ANPTIC"
            );
        }
    }

    private static Double parseNombre(String texte) {
        if (texte == null || texte.isBlank()) {
            return null;
        }
        String premierMot = texte.trim().split("\\s+")[0];
        try {
            return Double.parseDouble(premierMot);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private record EquipementRow(int objectId, Integer status, String technologie,
                                  String trafficEntrant, String trafficSortant) {
        static EquipementRow of(ResultSet rs, int rowNum) throws SQLException {
            return new EquipementRow(
                    rs.getInt("object_id"),
                    (Integer) rs.getObject("status"),
                    rs.getString("technologie"),
                    rs.getString("traffic_entrant"),
                    rs.getString("traffic_sortant")
            );
        }
    }

    private record DisponibiliteRow(long downSince, Double pourcentage) {}
}