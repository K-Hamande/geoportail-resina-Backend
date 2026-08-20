package bf.anptic.geoportail.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Date;

// Genere et valide les tokens JWT pour les utilisateurs decideurs.
// Separe du mecanisme Basic Auth utilise pour le Backoffice admin.
@Component
public class DecideurJwtService {

    private static final long EXPIRATION_MS = 8 * 60 * 60 * 1000; // 8 heures

    @Value("${resina.decideur.jwt-secret:decideur-secret-key-geoportail-resina-2026}")
    private String secret;

    private Key getKey() {
        byte[] keyBytes = Base64.getEncoder().encode(secret.getBytes());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String genererToken(Long userId, String login, String role, String ministere) {
        return Jwts.builder()
                .setSubject(login)
                .claim("userId", userId)
                .claim("role", role)
                .claim("ministere", ministere)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims validerToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean estValide(String token) {
        try {
            validerToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}