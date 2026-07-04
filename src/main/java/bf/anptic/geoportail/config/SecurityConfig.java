package bf.anptic.geoportail.config;

import bf.anptic.geoportail.security.RateLimitFilter;
import bf.anptic.geoportail.security.SiteAccessTokenFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final SiteAccessTokenFilter siteAccessTokenFilter;
    private final RateLimitFilter rateLimitFilter;

    public SecurityConfig(SiteAccessTokenFilter siteAccessTokenFilter, RateLimitFilter rateLimitFilter) {
        this.siteAccessTokenFilter = siteAccessTokenFilter;
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(headers -> headers
                    .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                    .contentTypeOptions(contentTypeOptions -> {})
                    .contentSecurityPolicy(csp -> csp.policyDirectives(
                            "default-src 'self'; frame-ancestors 'none'"))
            )
            .authorizeHttpRequests(authz -> authz
                    .requestMatchers("/api/v1/**").permitAll()
                    .requestMatchers("/mock-netxms/**").permitAll()
                    // NOUVEAU : le Backoffice exige une authentification
                    // (login/mot de passe), verifiee via BackofficeUserDetailsService
                    .requestMatchers("/backoffice/api/v1/**").authenticated()
                    .anyRequest().denyAll()
            )
            // httpBasic() active l'authentification HTTP Basic standard :
            // le navigateur/client envoie "Authorization: Basic base64(login:motdepasse)"
            .httpBasic(basic -> {})
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(siteAccessTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}