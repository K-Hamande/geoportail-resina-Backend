package bf.anptic.geoportail.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// Acces a l'API REST d'iTop (outil ITSM de l'ANPTIC) : source des tickets
// d'incident affiches au decideur sur la page "Alertes", a la place du
// calcul automatique depuis NetXMS utilise jusque-ici. Identifiants a
// fournir via application-dev.yml/application-prod.yml (gitignores) ou
// variables d'environnement, jamais commites en dur.
@Component
@ConfigurationProperties(prefix = "resina.itop")
public class ItopProperties {

    private String baseUrl;
    private String version = "1.0";
    private String login;
    private String password;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
