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
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
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
                    .requestMatchers("/error").permitAll()
                    .requestMatchers("/backoffice/api/v1/**").authenticated()
                    .anyRequest().denyAll()
            )
            .httpBasic(basic -> basic.authenticationEntryPoint((request, response, authException) -> {
                response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"message\":\"Authentification requise\"}");
            }))
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(siteAccessTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}