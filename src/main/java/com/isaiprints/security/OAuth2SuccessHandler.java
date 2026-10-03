package com.isaiprints.security;

import com.isaiprints.config.AppProperties;
import com.isaiprints.domain.entity.User;
import com.isaiprints.domain.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final AppProperties appProperties;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        User user = userRepository.findByGoogleSubjectId(oidcUser.getSubject())
                .orElseThrow(() -> new RuntimeException("User not found after OAuth2 login"));

        String token = jwtTokenProvider.generateToken(user);
        String frontendUrl = appProperties.getFrontendUrl();
        String redirectUrl = frontendUrl + "/auth/callback?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);

        log.info("[Auth] OAuth2 success for {}. Redirecting to frontend.", user.getEmail());
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
