package com.isaiprints.controller;

import com.isaiprints.domain.entity.Order;
import com.isaiprints.domain.entity.OrderItem;
import com.isaiprints.domain.entity.User;
import com.isaiprints.dto.request.CreateOrderRequest;
import com.isaiprints.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class OrderController {
    private final OrderService orderService;

    @PostMapping("/orders")
    public ResponseEntity<Map<String, Object>> createOrder(@Valid @RequestBody CreateOrderRequest req,
                                                            @AuthenticationPrincipal User user) {
        Order order = orderService.createOrder(req, user);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "orderNumber", order.getOrderNumber(),
            "totalAmount", order.getTotalAmount(),
            "currency", order.getCurrency(),
            "paymentMethod", order.getPaymentMethod().name(),
            "status", order.getStatus().name()
        ));
    }

    @GetMapping("/orders/{orderNumber}/status")
    public ResponseEntity<Map<String, Object>> getOrderStatus(@PathVariable String orderNumber,
                                                               @AuthenticationPrincipal User user) {
        Order order = orderService.findByOrderNumber(orderNumber);
        if (user != null && order.getUser() != null
                && !order.getUser().getId().equals(user.getId())
                && !user.getRole().name().equals("ROLE_ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }

        var itemsSummary = order.getItems().stream().map(item -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("filename", item.getOriginalFilename());
            map.put("fileType", item.getFileType() != null ? item.getFileType() : "IMAGE");
            map.put("pageCount", item.getPageCount() != null ? item.getPageCount() : 1);
            map.put("paperSize", item.getPaperSize().name());
            map.put("colorMode", item.getColorMode().name());
            map.put("copies", item.getCopies());
            map.put("unitPrice", item.getUnitPrice());
            map.put("subtotal", item.getTotalPrice());
            return map;
        }).collect(Collectors.toList());

        int totalCopies = order.getItems().stream().mapToInt(OrderItem::getCopies).sum();
        int totalPages = order.getItems().stream()
            .mapToInt(i -> (i.getPageCount() != null ? i.getPageCount() : 1) * i.getCopies()).sum();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("orderNumber", order.getOrderNumber());
        response.put("status", order.getStatus().name());
        response.put("paymentStatus", order.getPaymentStatus().name());
        response.put("paymentMethod", order.getPaymentMethod().name());
        response.put("paymentReference", order.getPaymentReference() != null ? order.getPaymentReference() : "");
        response.put("gatewayOrderId", order.getGatewayOrderId() != null ? order.getGatewayOrderId() : "");
        response.put("gatewayPaymentId", order.getGatewayPaymentId() != null ? order.getGatewayPaymentId() : "");
        response.put("paymentFailureReason", order.getPaymentFailureReason() != null ? order.getPaymentFailureReason() : "");
        response.put("totalAmount", order.getTotalAmount());
        response.put("currency", order.getCurrency());
        response.put("itemCount", order.getItems().size());
        response.put("totalCopies", totalCopies);
        response.put("totalPages", totalPages);
        response.put("itemsSummary", itemsSummary);
        response.put("createdAt", order.getCreatedAt().toString());
        response.put("updatedAt", order.getUpdatedAt().toString());

        return ResponseEntity.ok(response);
    }
}
