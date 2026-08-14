package bf.anptic.geoportail.security;

import bf.anptic.geoportail.config.ResinaProperties;
import bf.anptic.geoportail.model.MinistryAccessToken;
import bf.anptic.geoportail.repository.MinistryAccessTokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class SiteAccessTokenFilter extends OncePerRequestFilter {

    private final ResinaProperties properties;
    private final MinistryAccessTokenRepository ministryAccessTokenRepository;

    public SiteAccessTokenFilter(ResinaProperties properties,
                                  MinistryAccessTokenRepository ministryAccessTokenRepository) {
        this.properties = properties;
        this.ministryAccessTokenRepository = ministryAccessTokenRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain chain) throws ServletException, IOException {

        String uri = request.getRequestURI();

        if (!uri.startsWith("/api/v1")) {
            chain.doFilter(request, response);
            return;
        }

        String provided = request.getHeader("X-Resina-Site-Token");
        String jetonMaitre = properties.getAccessToken();

        try {
            if (jetonMaitre != null && jetonMaitre.equals(provided)) {
                AccessScopeHolder.setMinistere(null);
                chain.doFilter(request, response);
                return;
            }

            if (provided != null) {
                Optional<MinistryAccessToken> jeton = ministryAccessTokenRepository.findByTokenAndActifTrue(provided);
                if (jeton.isPresent()) {
                    AccessScopeHolder.setMinistere(jeton.get().getMinistere());
                    chain.doFilter(request, response);
                    return;
                }
            }

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"Accès refusé. Token manquant ou invalide.\"}");
        } finally {
            AccessScopeHolder.clear();
        }
    }
}