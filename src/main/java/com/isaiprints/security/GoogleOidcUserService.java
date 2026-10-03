package com.isaiprints.security;

import com.isaiprints.config.AppProperties;
import com.isaiprints.domain.entity.User;
import com.isaiprints.domain.enums.UserRole;
import com.isaiprints.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleOidcUserService extends OidcUserService {
    private final UserRepository userRepository;
    private final AppProperties appProperties;

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);
        String googleSubjectId = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName();
        String picture = oidcUser.getPicture();

        boolean isAdmin = appProperties.getAdminEmails().stream()
                .map(String::trim).map(String::toLowerCase)
                .anyMatch(e -> e.equals(email != null ? email.toLowerCase() : ""));

        User user = userRepository.findByGoogleSubjectId(googleSubjectId)
                .orElseGet(() -> User.builder()
                        .googleSubjectId(googleSubjectId)
                        .email(email)
                        .build());

        user.setName(name);
        user.setPictureUrl(picture);
        if (user.getId() == null) {
            user.setRole(isAdmin ? UserRole.ROLE_ADMIN : UserRole.ROLE_USER);
        } else if (isAdmin && user.getRole() != UserRole.ROLE_ADMIN) {
            user.setRole(UserRole.ROLE_ADMIN);
        }

        userRepository.save(user);
        log.info("[Auth] Google login: {} ({})", email, user.getRole());
        return oidcUser;
    }
}
