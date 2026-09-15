package bf.anptic.geoportail.service;

import bf.anptic.geoportail.dto.CommuneCouvertureDto;
import bf.anptic.geoportail.dto.CouvertureStatsDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Page publique "Geoportail national" (§ hors device decideur - aucune
// authentification requise) : couverture RESINA commune par commune,
// a partir des tables SIG deja accessibles en lecture via netxmsdb
// (schemas donnebase/equipementinfastructure, cf. NetxmsDataSourceConfig).
//
// Reprend la logique de la vue PostGIS fournie en reference
// (donnebase.vue_commune_connectee_resina - "connectee" = contient un
// site administratif raccorde OU est traversee par une liaison), mais
// en SQL direct plutot que de dependre de la creation effective de cette
// vue dans netxmsdb : ce schema est un systeme externe en lecture seule,
// on n'y cree jamais d'objet (cf. commentaire NetxmsDataSourceConfig).
// La distinction CONNECTEE (a un site) / PARTIELLE (seulement traversee)
// n'existe pas dans la vue elle-meme (elle fusionne les deux avec un
// simple OR) mais est necessaire pour la legende deux couleurs de la
// page publique - calculee ici a partir des deux memes conditions.
@Service
public class CouvertureNationaleService {

    // id_lcommune=124 (ZIOU, province NAHOURI) est un doublon d'erreur de
    // saisie SIG : deux polygones existent pour la meme commune (meme
    // "superficie" declaree de 269 km2 dans les deux lignes), mais 124 est
    // un fragment orphelin de 15 km2 sans aucun site ni ville rattaches,
    // tandis que 125 (254 km2) est la commune reellement utilisee ailleurs
    // dans la base (donnebase.ville y pointe, un site administratif y est
    // rattache). Exclu de toutes les requetes ci-dessous pour retrouver le
    // decompte officiel de 351 communes plutot que 352.
    private static final String COMMUNE_VALIDE = "c.id_lcommune <> 124";

    private static final String SELECT_STATS = """
            SELECT
              (SELECT count(*) FROM donnebase.limitecommune c WHERE %1$s) AS total_communes,
              (SELECT count(*) FROM donnebase.limitecommune c
                 WHERE %1$s
                   AND (EXISTS (SELECT 1 FROM donnebase.siteadministratif s
                               WHERE s.connectionresina = 'Oui' AND ST_Contains(c.geom, s.geom))
                    OR EXISTS (SELECT 1 FROM equipementinfastructure."Liaison" l
                               WHERE ST_Intersects(c.geom, l.geom)))
              ) AS communes_connectees,
              (SELECT count(*) FROM donnebase.siteadministratif WHERE connectionresina = 'Oui') AS sites_raccordes,
              (SELECT COALESCE(round(sum(ST_Length(geom::geography)) / 1000), 0)
                 FROM equipementinfastructure."Liaison" WHERE geom IS NOT NULL) AS km_liaisons
            """.formatted(COMMUNE_VALIDE);

    private static final String SELECT_COMMUNES = """
            SELECT
              c.id_lcommune,
              c.nomcommune,
              p.nomprovince,
              r.nomregion,
              (SELECT count(*) FROM donnebase.siteadministratif s
                 WHERE s.connectionresina = 'Oui' AND ST_Contains(c.geom, s.geom)) AS nb_sites_connectes,
              EXISTS (SELECT 1 FROM equipementinfastructure."Liaison" l
                      WHERE ST_Intersects(c.geom, l.geom)) AS a_liaison
            FROM donnebase.limitecommune c
            LEFT JOIN donnebase.limiteprovince p ON p.id_lprovince = c.id_lprovince
            LEFT JOIN donnebase.limiteregion r ON r.id_lregion = p.id_lregion
            WHERE %s
            ORDER BY c.nomcommune
            """.formatted(COMMUNE_VALIDE);

    // ST_SimplifyPreserveTopology reduit drastiquement le poids des
    // polygones (verifie sur les donnees reelles : ~76 Ko -> ~2 Ko par
    // commune en moyenne, soit un chargement total de ~700 Ko au lieu de
    // ~27 Mo) - une tolerance de 0.0015 degre (~150 m) reste largement
    // suffisante pour une carte de couverture nationale, la precision
    // cadastrale du trace n'ayant pas d'interet a ce niveau de zoom.
    private static final String SELECT_COMMUNES_GEOJSON = """
            SELECT
              c.id_lcommune,
              c.nomcommune,
              ST_AsGeoJSON(ST_SimplifyPreserveTopology(c.geom, 0.0015)) AS geojson,
              (SELECT count(*) FROM donnebase.siteadministratif s
                 WHERE s.connectionresina = 'Oui' AND ST_Contains(c.geom, s.geom)) AS nb_sites_connectes,
              EXISTS (SELECT 1 FROM equipementinfastructure."Liaison" l
                      WHERE ST_Intersects(c.geom, l.geom)) AS a_liaison
            FROM donnebase.limitecommune c
            WHERE %s
            """.formatted(COMMUNE_VALIDE);

    // Sites administratifs effectivement raccordes (points) - superpose a
    // la carte des communes pour montrer la localisation precise des
    // sites, pas seulement la commune qui les contient.
    private static final String SELECT_SITES_GEOJSON = """
            SELECT
              id_siteadministratif,
              nomsiteadministratif,
              "Ministère" AS ministere,
              ST_AsGeoJSON(geom) AS geojson
            FROM donnebase.siteadministratif
            WHERE connectionresina = 'Oui' AND geom IS NOT NULL
            """;

    // Liaisons fibre optique (lignes) - meme simplification de principe que
    // les polygones de commune : verifie sur les donnees reelles, une
    // tolerance de 0.0005 degre (~50 m) fait passer le poids total de
    // ~2.8 Mo a ~350 Ko sans alterer le trace a l'echelle nationale.
    private static final String SELECT_LIAISONS_GEOJSON = """
            SELECT
              id_liaison,
              nomligne,
              ST_AsGeoJSON(ST_SimplifyPreserveTopology(geom, 0.0005)) AS geojson
            FROM equipementinfastructure."Liaison"
            WHERE geom IS NOT NULL
            """;

    private final JdbcTemplate netxmsJdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CouvertureNationaleService(@Qualifier("netxmsJdbcTemplate") JdbcTemplate netxmsJdbcTemplate) {
        this.netxmsJdbcTemplate = netxmsJdbcTemplate;
    }

    public CouvertureStatsDto getStats() {
        return netxmsJdbcTemplate.queryForObject(SELECT_STATS, (rs, rowNum) -> new CouvertureStatsDto(
                rs.getLong("total_communes"),
                rs.getLong("communes_connectees"),
                rs.getLong("sites_raccordes"),
                rs.getDouble("km_liaisons")
        ));
    }

    public List<CommuneCouvertureDto> listCommunes() {
        return netxmsJdbcTemplate.query(SELECT_COMMUNES, (rs, rowNum) -> {
            long nbSites = rs.getLong("nb_sites_connectes");
            boolean aLiaison = rs.getBoolean("a_liaison");
            return new CommuneCouvertureDto(
                    rs.getInt("id_lcommune"),
                    rs.getString("nomcommune"),
                    rs.getString("nomprovince"),
                    rs.getString("nomregion"),
                    statutDe(nbSites, aLiaison),
                    nbSites
            );
        });
    }

    // FeatureCollection GeoJSON standard, directement exploitable par un
    // calque Leaflet (L.geoJSON) cote frontend.
    public Map<String, Object> getCommunesGeoJson() {
        List<Map<String, Object>> features = new ArrayList<>();

        netxmsJdbcTemplate.query(SELECT_COMMUNES_GEOJSON, (rs) -> {
            long nbSites = rs.getLong("nb_sites_connectes");
            boolean aLiaison = rs.getBoolean("a_liaison");

            JsonNode geometrie;
            try {
                geometrie = objectMapper.readTree(rs.getString("geojson"));
            } catch (Exception e) {
                return; // geometrie illisible pour cette commune : on la saute plutot que de faire echouer toute la carte
            }

            Map<String, Object> proprietes = new LinkedHashMap<>();
            proprietes.put("id", rs.getInt("id_lcommune"));
            proprietes.put("nom", rs.getString("nomcommune"));
            proprietes.put("statut", statutDe(nbSites, aLiaison));
            proprietes.put("nombreSitesConnectes", nbSites);

            Map<String, Object> feature = new LinkedHashMap<>();
            feature.put("type", "Feature");
            feature.put("geometry", geometrie);
            feature.put("properties", proprietes);
            features.add(feature);
        });

        Map<String, Object> featureCollection = new LinkedHashMap<>();
        featureCollection.put("type", "FeatureCollection");
        featureCollection.put("features", features);
        return featureCollection;
    }

    // FeatureCollection de points : un site administratif raccorde par
    // Feature, pour le calque "site administratif connecte" de la carte.
    public Map<String, Object> getSitesGeoJson() {
        List<Map<String, Object>> features = new ArrayList<>();

        netxmsJdbcTemplate.query(SELECT_SITES_GEOJSON, (rs) -> {
            JsonNode geometrie;
            try {
                geometrie = objectMapper.readTree(rs.getString("geojson"));
            } catch (Exception e) {
                return; // geometrie illisible pour ce site : on le saute plutot que de faire echouer toute la carte
            }

            Map<String, Object> proprietes = new LinkedHashMap<>();
            proprietes.put("id", rs.getInt("id_siteadministratif"));
            proprietes.put("nom", rs.getString("nomsiteadministratif"));
            proprietes.put("ministere", rs.getString("ministere"));

            Map<String, Object> feature = new LinkedHashMap<>();
            feature.put("type", "Feature");
            feature.put("geometry", geometrie);
            feature.put("properties", proprietes);
            features.add(feature);
        });

        Map<String, Object> featureCollection = new LinkedHashMap<>();
        featureCollection.put("type", "FeatureCollection");
        featureCollection.put("features", features);
        return featureCollection;
    }

    // FeatureCollection de lignes : une liaison fibre optique par Feature,
    // pour le calque "fibre optique" de la carte.
    public Map<String, Object> getLiaisonsGeoJson() {
        List<Map<String, Object>> features = new ArrayList<>();

        netxmsJdbcTemplate.query(SELECT_LIAISONS_GEOJSON, (rs) -> {
            JsonNode geometrie;
            try {
                geometrie = objectMapper.readTree(rs.getString("geojson"));
            } catch (Exception e) {
                return; // geometrie illisible pour cette liaison : on la saute plutot que de faire echouer toute la carte
            }

            Map<String, Object> proprietes = new LinkedHashMap<>();
            proprietes.put("id", rs.getInt("id_liaison"));
            proprietes.put("nom", rs.getString("nomligne"));

            Map<String, Object> feature = new LinkedHashMap<>();
            feature.put("type", "Feature");
            feature.put("geometry", geometrie);
            feature.put("properties", proprietes);
            features.add(feature);
        });

        Map<String, Object> featureCollection = new LinkedHashMap<>();
        featureCollection.put("type", "FeatureCollection");
        featureCollection.put("features", features);
        return featureCollection;
    }

    private static String statutDe(long nombreSitesConnectes, boolean traverseeParLiaison) {
        if (nombreSitesConnectes > 0) return "CONNECTEE";
        if (traverseeParLiaison) return "PARTIELLE";
        return "NON_CONNECTEE";
    }
}
