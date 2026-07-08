package bf.anptic.geoportail.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "resina.bootstrap")
public class BootstrapProperties {

    private String adminLogin;
    private String adminPassword;
    private String adminNomComplet;

    public String getAdminLogin() {
        return adminLogin;
    }

    public void setAdminLogin(String adminLogin) {
        this.adminLogin = adminLogin;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public String getAdminNomComplet() {
        return adminNomComplet;
    }

    public void setAdminNomComplet(String adminNomComplet) {
        this.adminNomComplet = adminNomComplet;
    }
}