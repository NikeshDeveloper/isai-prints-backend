package com.isaiprints.domain.repository;

import com.isaiprints.domain.entity.Order;
import com.isaiprints.domain.enums.OrderStatus;
import com.isaiprints.domain.enums.PaymentMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    Optional<Order> findByOrderNumber(String orderNumber);
    Optional<Order> findByGatewayPaymentId(String gatewayPaymentId);
    boolean existsByGatewayPaymentId(String gatewayPaymentId);
    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<Order> findByStatusAndPaymentMethod(OrderStatus status, PaymentMethod paymentMethod);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :since")
    long countOrdersSince(Instant since);
    
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.paymentStatus = 'PAID' AND o.createdAt >= :since")
    java.math.BigDecimal sumRevenueToday(Instant since);
}
