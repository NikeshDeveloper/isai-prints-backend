package com.isaiprints.controller;

import com.isaiprints.domain.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final com.isaiprints.security.JwtTokenProvider jwtTokenProvider;
    private final com.isaiprints.domain.repository.UserRepository userRepository;

    public AuthController(com.isaiprints.security.JwtTokenProvider jwtTokenProvider, com.isaiprints.domain.repository.UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
    }

    @PostMapping("/dev-login")
    public ResponseEntity<Map<String, String>> devLogin(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setEmail(email);
            u.setName("Dev User");
            u.setGoogleSubjectId("dev-" + email);
            u.setRole(com.isaiprints.domain.enums.UserRole.ROLE_ADMIN);
            return userRepository.save(u);
        });
        return ResponseEntity.ok(Map.of("token", jwtTokenProvider.generateToken(user)));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMe(@AuthenticationPrincipal User user) {
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        return ResponseEntity.ok(Map.of(
            "id", user.getId().toString(),
            "email", user.getEmail(),
            "name", user.getName() != null ? user.getName() : "",
            "picture", user.getPictureUrl() != null ? user.getPictureUrl() : "",
            "role", user.getRole().name()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
