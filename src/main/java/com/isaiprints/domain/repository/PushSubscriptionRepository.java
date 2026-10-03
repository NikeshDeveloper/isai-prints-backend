package com.isaiprints.domain.repository;

import com.isaiprints.domain.entity.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, UUID> {
    Optional<PushSubscription> findByEndpoint(String endpoint);
    void deleteByEndpoint(String endpoint);
}
