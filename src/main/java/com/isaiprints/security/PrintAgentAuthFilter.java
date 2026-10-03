package com.isaiprints.security;

import com.isaiprints.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PrintAgentAuthFilter extends OncePerRequestFilter {
    private final AppProperties appProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api/print-agent/")) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = extractToken(request);
        String agentToken = appProperties.getPrintAgentToken();
        if (token != null && token.equals(agentToken)) {
            var auth = new UsernamePasswordAuthenticationToken(
                    "print-agent", null,
                    List.of(new SimpleGrantedAuthority("ROLE_PRINT_AGENT"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) return bearer.substring(7);
        return request.getHeader("X-Print-Agent-Token");
    }
}
