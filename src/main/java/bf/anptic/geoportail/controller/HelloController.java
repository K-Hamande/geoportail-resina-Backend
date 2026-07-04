package bf.anptic.geoportail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    // @Value injecte automatiquement la valeur lue dans les fichiers YAML,
    // en cherchant la cle "resina.message"
    @Value("${resina.message}")
    private String message;

    @GetMapping("/hello")
    public String hello() {
        return "Géoportail RESINA - API en ligne";
    }

    @GetMapping("/profile-check")
    public String profileCheck() {
        return message;
    }
}