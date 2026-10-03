package com.isaiprints.config;

import com.isaiprints.security.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;
    private final PrintAgentAuthFilter printAgentAuthFilter;
    private final GoogleOidcUserService googleOidcUserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final AppProperties appProperties;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/pricing").permitAll()
                .requestMatchers("/api/payments/webhook").permitAll()
                .requestMatchers("/api/auth/**", "/oauth2/**", "/login/**").permitAll()
                // Order creation + status: allow unauthenticated (kiosk self-service)
                .requestMatchers(HttpMethod.POST, "/api/orders").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/orders/*/status").permitAll()
                // Payment creation: allow unauthenticated (kiosk pays after creating order)
                .requestMatchers(HttpMethod.POST, "/api/payments/**").permitAll()
                // Admin endpoints: only ROLE_ADMIN JWT users
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // Print agent endpoints: agent token auth
                .requestMatchers("/api/print-agent/**").hasRole("PRINT_AGENT")
                // Everything else requires JWT auth
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(ui -> ui.oidcUserService(googleOidcUserService))
                .successHandler(oAuth2SuccessHandler)
            )
            .addFilterBefore(printAgentAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
            "http://localhost:5173",
            "http://localhost:3000",
            "https://isai-prints.ai.studio",
            appProperties.getFrontendUrl()
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
