package com.isaiprints.controller;

import com.isaiprints.domain.entity.Order;
import com.isaiprints.domain.entity.User;
import com.isaiprints.domain.enums.PaymentMethod;
import com.isaiprints.domain.enums.PrintJobStatus;
import com.isaiprints.domain.repository.AuditLogRepository;
import com.isaiprints.domain.repository.OrderRepository;
import com.isaiprints.domain.repository.PrintJobRepository;
import com.isaiprints.domain.repository.PushSubscriptionRepository;
import com.isaiprints.domain.entity.PushSubscription;
import com.isaiprints.dto.request.PushSubscribeRequest;
import com.isaiprints.service.AuditLogService;
import com.isaiprints.service.NotificationService;
import com.isaiprints.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final PrintJobRepository printJobRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationService notificationService;
    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final AuditLogService auditLogService;

    @GetMapping("/metrics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        Instant startOfToday = Instant.now().truncatedTo(ChronoUnit.DAYS);
        return ResponseEntity.ok(Map.of(
            "todayOrdersCount", orderRepository.countOrdersSince(startOfToday),
            "todayRevenue", orderRepository.sumRevenueToday(startOfToday),
            "pendingCashOrdersCount", orderRepository
                .findByStatusAndPaymentMethod(com.isaiprints.domain.enums.OrderStatus.PAYMENT_PENDING, PaymentMethod.CASH).size(),
            "queuedPrintsCount", printJobRepository.countByStatus(PrintJobStatus.QUEUED),
            "printingCount", printJobRepository.countByStatus(PrintJobStatus.PRINTING),
            "completedPrintsCount", printJobRepository.countByStatus(PrintJobStatus.COMPLETED),
            "failedPrintsCount", printJobRepository.countByStatus(PrintJobStatus.FAILED)
        ));
    }

    @GetMapping("/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var ordersPage = orderRepository.findAllByOrderByCreatedAtDesc(pageable);
        var orders = ordersPage.getContent().stream().map(this::mapOrderToSummary).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of(
            "orders", orders,
            "totalElements", ordersPage.getTotalElements(),
            "totalPages", ordersPage.getTotalPages()
        ));
    }

    @PostMapping("/orders/{orderNumber}/verify-cash")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> verifyCash(@PathVariable String orderNumber,
                                                            @AuthenticationPrincipal User admin) {
        Order order = orderService.verifyCash(orderNumber, admin);
        auditLogService.log(admin != null ? admin.getId().toString() : "ADMIN", "ADMIN", "CASH_VERIFIED", "Order", orderNumber, "SUCCESS",
            "Cash payment verified for order " + orderNumber);
        return ResponseEntity.ok(Map.of(
            "success", true, "orderNumber", orderNumber, "status", order.getStatus().name()
        ));
    }

    @PostMapping("/orders/{orderNumber}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> cancelOrder(@PathVariable String orderNumber,
                                                            @AuthenticationPrincipal User admin) {
        Order order = orderService.cancelOrder(orderNumber, admin);
        return ResponseEntity.ok(Map.of("success", true, "status", order.getStatus().name()));
    }

    @GetMapping("/push/vapid-key")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getVapidKey() {
        return ResponseEntity.ok(Map.of("vapidPublicKey", notificationService.getVapidPublicKey()));
    }

    @PostMapping("/push/subscribe")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> subscribe(@RequestBody PushSubscribeRequest req,
                                                          @AuthenticationPrincipal User user) {
        var existing = pushSubscriptionRepository.findByEndpoint(req.getEndpoint());
        if (existing.isEmpty()) {
            PushSubscription sub = PushSubscription.builder()
                .endpoint(req.getEndpoint())
                .p256dh(req.getP256dh())
                .auth(req.getAuth())
                .user(user)
                .build();
            pushSubscriptionRepository.save(sub);
        }
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public SseEmitter subscribeEvents() {
        return notificationService.createAdminSseEmitter();
    }

    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var logsPage = auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        var logs = logsPage.getContent().stream().map(l -> Map.<String, Object>of(
            "id", l.getId().toString(),
            "actorId", l.getActorId() != null ? l.getActorId() : "",
            "actorType", l.getActorType() != null ? l.getActorType() : "",
            "action", l.getAction(),
            "entityType", l.getEntityType() != null ? l.getEntityType() : "",
            "result", l.getResult(),
            "message", l.getMessage() != null ? l.getMessage() : "",
            "timestamp", l.getCreatedAt().toString()
        )).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("logs", logs, "totalElements", logsPage.getTotalElements()));
    }

    private Map<String, Object> mapOrderToSummary(Order order) {
        return Map.of(
            "orderNumber", order.getOrderNumber(),
            "status", order.getStatus().name(),
            "paymentMethod", order.getPaymentMethod().name(),
            "paymentStatus", order.getPaymentStatus().name(),
            "totalAmount", order.getTotalAmount(),
            "itemCount", order.getItems().size(),
            "createdAt", order.getCreatedAt().toString(),
            "updatedAt", order.getUpdatedAt().toString()
        );
    }
}
