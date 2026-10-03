package com.isaiprints.domain.repository;

import com.isaiprints.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByGoogleSubjectId(String googleSubjectId);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
