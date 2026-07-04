package bf.anptic.geoportail.security;

import bf.anptic.geoportail.config.ResinaProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

// §4.4 du CDC : "Maximum 10 requetes / minute / IP".
// Implementation "fenetre fixe" : pour chaque IP, on compte les requetes
// recues dans la minute en cours ; le compteur repart a zero des que
// la minute change.
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final ResinaProperties properties;

    // Une entree par IP. ConcurrentHashMap : version thread-safe de HashMap,
    // necessaire car plusieurs requetes simultanees peuvent lire/ecrire
    // cette map en meme temps.
    private final ConcurrentMap<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimitFilter(ResinaProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain chain) throws ServletException, IOException {

        if (!request.getRequestURI().startsWith("/api/v1")) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = request.getRemoteAddr();
        Window window = windows.computeIfAbsent(clientIp, ip -> new Window());

        // Numero de la minute courante depuis 1970 (ex: 29234521).
        // Deux requetes dans la MEME minute ont le meme windowId.
        long currentWindowId = System.currentTimeMillis() / 60_000L;

        if (window.windowId.get() != currentWindowId) {
            // Nouvelle minute : on remet le compteur a zero
            window.windowId.set(currentWindowId);
            window.count.set(0);
        }

        int newCount = window.count.incrementAndGet();

        if (newCount <= properties.getRateLimitCapacity()) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(429);   // 429 = "Too Many Requests"
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"Trop de requêtes. Veuillez réessayer dans un instant.\"}");
        }
    }

    // Petite classe interne : regroupe le numero de minute + le compteur
    // pour une IP donnee.
    private static final class Window {
        final AtomicLong windowId = new AtomicLong(-1);
        final AtomicInteger count = new AtomicInteger(0);
    }
}