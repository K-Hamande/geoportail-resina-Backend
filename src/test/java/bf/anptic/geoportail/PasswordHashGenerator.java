package bf.anptic.geoportail;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

// Fichier TEMPORAIRE : sert uniquement a generer un hash BCrypt
// a coller manuellement dans un INSERT SQL. A supprimer une fois utilise.
public class PasswordHashGenerator {

    @Test
    void generateHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode("ChangeMe123!");
        System.out.println("HASH GENERE : " + hash);
    }
}