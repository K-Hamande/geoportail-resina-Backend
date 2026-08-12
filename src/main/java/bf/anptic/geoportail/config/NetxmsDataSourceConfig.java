package bf.anptic.geoportail.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

// Deuxieme datasource, dediee a la lecture de netxmsdb (vues geo_*).
// Volontairement SEPAREE du DataSource principal (spring.datasource) qui,
// lui, est gere par JPA/Hibernate sur geoportail_resina_db.
// Ici pas de JPA : netxmsdb n'est interrogee qu'en lecture seule via JdbcTemplate,
// les vues geo_* n'ayant pas vocation a devenir des entites gerees par Hibernate.
@Configuration
public class NetxmsDataSourceConfig {

    private final NetxmsDataSourceProperties properties;

    public NetxmsDataSourceConfig(NetxmsDataSourceProperties properties) {
        this.properties = properties;
    }

    @Bean(name = "netxmsDataSource")
    public DataSource netxmsDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(properties.getUrl());
        config.setUsername(properties.getUsername());
        config.setPassword(properties.getPassword());
        config.setDriverClassName(properties.getDriverClassName());
        // Pool volontairement petit : ce datasource ne sert qu'a des lectures
        // ponctuelles (import, consultation temps reel par site), pas au trafic principal.
        config.setMaximumPoolSize(5);
        config.setPoolName("netxms-readonly-pool");
        config.setReadOnly(true);
        return new HikariDataSource(config);
    }

    // @Qualifier explicite obligatoire : depuis que PrimaryDataSourceConfig
    // marque l'autre DataSource en @Primary, Spring privilegierait ce dernier
    // par defaut en cas d'ambiguite, meme si le nom du parametre correspond
    // exactement au bean "netxmsDataSource". Sans ce @Qualifier, ce JdbcTemplate
    // se connectait silencieusement a geoportail_resina_db au lieu de netxmsdb.
    @Bean(name = "netxmsJdbcTemplate")
    public JdbcTemplate netxmsJdbcTemplate(@Qualifier("netxmsDataSource") DataSource netxmsDataSource) {
        return new JdbcTemplate(netxmsDataSource);
    }
}