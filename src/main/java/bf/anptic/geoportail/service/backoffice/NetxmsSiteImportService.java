package bf.anptic.geoportail.service.backoffice;

import bf.anptic.geoportail.model.Site;
import bf.anptic.geoportail.repository.SiteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

// Importe/synchronise le catalogue des sites RESINA depuis netxmsdb
// (donnebase.siteadministratif filtree sur connectionresina = 'Oui')
// vers la table applicative geoportail_resina.sites.
//
// Comportement upsert :
//  - site absent  -> creation, actif = true par defaut
//  - site present -> mise a jour des SEULS champs dont netxmsdb est la source
//                    de verite (nom, ville, region, coordonnees, ministere/structure).
//                    Les champs geres a la main dans le backoffice (batiment, niveaux,
//                    contacts DSI, actif/inactif) ne sont JAMAIS ecrases par un reimport.
@Service
public class NetxmsSiteImportService {

    private static final Logger log = LoggerFactory.getLogger(NetxmsSiteImportService.class);

    // Un site RESINA est identifie par connectionresina = 'Oui' dans
    // donnebase.siteadministratif. La region est obtenue par la chaine de
    // jointures ville -> commune -> province -> region (meme logique que
    // celle utilisee par la vue public.geo_equipement).
    private static final String SELECT_SITES_RESINA = """
            SELECT sa.id_siteadministratif AS id,
                   sa.nomsiteadministratif AS nom,
                   sa.latitude             AS latitude,
                   sa.longitude            AS longitude,
                   sa.structure            AS structure,
                   sa."Minist\u00E8re"     AS ministere,
                   vi.nomville             AS nomville,
                   re.nomregion            AS nomregion
            FROM donnebase.siteadministratif sa
            LEFT JOIN donnebase.ville vi ON sa.id_ville = vi.id_ville
            LEFT JOIN donnebase.limitecommune co ON vi.id_commune = co.id_lcommune
            LEFT JOIN donnebase.limiteprovince po ON co.id_lprovince = po.id_lprovince
            LEFT JOIN donnebase.limiteregion re ON po.id_lregion = re.id_lregion
            WHERE sa.connectionresina = 'Oui'
            ORDER BY sa.id_siteadministratif
            """;

    private final JdbcTemplate netxmsJdbcTemplate;
    private final SiteRepository siteRepository;

    public NetxmsSiteImportService(@Qualifier("netxmsJdbcTemplate") JdbcTemplate netxmsJdbcTemplate,
                                    SiteRepository siteRepository) {
        this.netxmsJdbcTemplate = netxmsJdbcTemplate;
        this.siteRepository = siteRepository;
    }

    public static class ImportResult {
        public int crees;
        public int misAJour;
        public int total;
    }

    @Transactional
    public ImportResult importSitesResina() {
        List<Site> sitesSource = netxmsJdbcTemplate.query(SELECT_SITES_RESINA, (rs, rowNum) -> {
            int idSiteAdministratif = rs.getInt("id");

            Site site = new Site();
            site.setSiteId(String.valueOf(idSiteAdministratif));
            site.setNom(rs.getString("nom"));
            site.setVille(rs.getString("nomville"));
            site.setRegionAdministrative(rs.getString("nomregion"));

            double lat = rs.getDouble("latitude");
            if (!rs.wasNull()) {
                site.setLatitude(lat);
            }
            double lon = rs.getDouble("longitude");
            if (!rs.wasNull()) {
                site.setLongitude(lon);
            }

            site.setNetxmsNodeId(idSiteAdministratif);

            String structure = rs.getString("structure");
            String ministere = rs.getString("ministere");
            site.setInfoAuSurvol(buildInfoAuSurvol(structure, ministere));
            site.setMinistere(ministere);

            return site;
        });

        ImportResult result = new ImportResult();
        result.total = sitesSource.size();

        for (Site source : sitesSource) {
            Optional<Site> existant = siteRepository.findById(source.getSiteId());

            if (existant.isPresent()) {
                Site site = existant.get();
                site.setNom(source.getNom());
                site.setVille(source.getVille());
                site.setRegionAdministrative(source.getRegionAdministrative());
                site.setLatitude(source.getLatitude());
                site.setLongitude(source.getLongitude());
                site.setNetxmsNodeId(source.getNetxmsNodeId());
                site.setInfoAuSurvol(source.getInfoAuSurvol());
                site.setMinistere(source.getMinistere());
                // volontairement PAS touche : batiment, niveaux, contactDsiNom,
                // contactDsiTelephone, actif -> geres a la main dans le backoffice
                siteRepository.save(site);
                result.misAJour++;
            } else {
                source.setActif(true);
                siteRepository.save(source);
                result.crees++;
            }
        }

        log.info("Import sites RESINA termine : {} crees, {} mis a jour, {} au total",
                result.crees, result.misAJour, result.total);

        return result;
    }

    private static String buildInfoAuSurvol(String structure, String ministere) {
        boolean hasStructure = structure != null && !structure.isBlank();
        boolean hasMinistere = ministere != null && !ministere.isBlank();

        if (hasStructure && hasMinistere) {
            return structure + " - " + ministere;
        }
        if (hasStructure) {
            return structure;
        }
        if (hasMinistere) {
            return ministere;
        }
        return null;
    }
}