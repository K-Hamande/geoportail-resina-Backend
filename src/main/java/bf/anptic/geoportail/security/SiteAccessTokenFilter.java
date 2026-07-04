package bf.anptic.geoportail.security;

import bf.anptic.geoportail.config.ResinaProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// OncePerRequestFilter garantit que ce filtre s'execute une seule fois
// par requete (utile car certains mecanismes internes de Servlet peuvent
// sinon redeclencher un filtre plusieurs fois).
@Component
public class SiteAccessTokenFilter extends OncePerRequestFilter {

    private final ResinaProperties properties;

    public SiteAccessTokenFilter(ResinaProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain chain) throws ServletException, IOException {

        String uri = request.getRequestURI();

        // On ne protege que les endpoints decideur /api/v1/**
        // (le mock NetXMS et le reste ne sont pas concernes ici)
        if (!uri.startsWith("/api/v1")) {
            chain.doFilter(request, response);   // laisse passer, sans verification
            return;
        }

        String provided = request.getHeader("X-Resina-Site-Token");
        String expected = properties.getAccessToken();

        if (expected != null && expected.equals(provided)) {
            chain.doFilter(request, response);   // token correct : on continue vers le controleur
            return;
        }

        // Token absent ou incorrect : on coupe court, sans jamais atteindre le controleur
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"Accès refusé. Token manquant ou invalide.\"}");
    }
}