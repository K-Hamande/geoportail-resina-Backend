package bf.anptic.geoportail.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

// Rend explicite et non-ambigu le DataSource principal (geoportail_resina_db),
// utilise par JPA/Hibernate. Necessaire depuis qu'un second DataSource
// (netxmsDataSource, voir NetxmsDataSourceConfig) existe dans le contexte :
// sans @Primary ici, Spring ne sait plus lequel des deux injecter dans JPA.
//
// On passe par DataSourceProperties (et non DataSourceBuilder directement)
// car c'est cette classe qui sait traduire correctement la propriete "url"
// du YAML vers le bon setter du pool sous-jacent (ex: setJdbcUrl() pour Hikari).
@Configuration
public class PrimaryDataSourceConfig {

    @Primary
    @Bean(name = "dataSourceProperties")
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean(name = "dataSource")
    public DataSource dataSource(DataSourceProperties dataSourceProperties) {
        return dataSourceProperties.initializeDataSourceBuilder().build();
    }
}