package com.isaiprints.service;

import com.isaiprints.domain.entity.*;
import com.isaiprints.domain.enums.*;
import com.isaiprints.domain.repository.*;
import com.isaiprints.dto.request.CreateOrderRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PricingService pricingService;
    private final PrintJobService printJobService;
    private final PdfService pdfService;
    private final StorageService storageService;
    private final FileAssetRepository fileAssetRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    @Transactional
    public Order createOrder(CreateOrderRequest req, User user) {
        String orderNumber = generateOrderNumber();
        BigDecimal subtotal = BigDecimal.ZERO;

        Order order = Order.builder()
            .orderNumber(orderNumber)
            .user(user)
            .paymentMethod(req.getPaymentMethod())
            .status(OrderStatus.PAYMENT_PENDING)
            .paymentStatus(PaymentStatus.PENDING)
            .currency("INR")
            .subtotal(BigDecimal.ZERO)
            .totalAmount(BigDecimal.ZERO)
            .items(new ArrayList<>())
            .build();

        order = orderRepository.save(order);

        for (CreateOrderRequest.OrderItemRequest itemReq : req.getItems()) {
            PaperSize paperSize = PaperSize.valueOf(itemReq.getPaperSize());
            ColorMode colorMode = ColorMode.valueOf(itemReq.getColorMode());
            int copies = Math.max(1, Math.min(20, itemReq.getCopies()));

            int pageCount = 1;
            if ("PDF".equalsIgnoreCase(itemReq.getFileType()) || 
                (itemReq.getMimeType() != null && itemReq.getMimeType().contains("pdf"))) {
                try {
                    byte[] fileBytes = storageService.download(itemReq.getStorageKey());
                    pageCount = pdfService.getPageCount(fileBytes);
                } catch (Exception e) {
                    log.warn("Could not determine PDF page count for {}: {}", itemReq.getStorageKey(), e.getMessage());
                    pageCount = Math.max(1, itemReq.getPageCount() != null ? itemReq.getPageCount() : 1);
                }
            }

            BigDecimal unitPrice = pricingService.getUnitPrice(paperSize, colorMode);
            BigDecimal itemTotal = pricingService.calculateItemPrice(paperSize, colorMode, copies, pageCount);
            subtotal = subtotal.add(itemTotal);

            OrderItem item = OrderItem.builder()
                .order(order)
                .originalFilename(itemReq.getOriginalFilename())
                .storageKey(itemReq.getStorageKey())
                .mimeType(itemReq.getMimeType() != null ? itemReq.getMimeType() : "application/octet-stream")
                .fileType(itemReq.getFileType())
                .fileSize(itemReq.getFileSize())
                .pageCount(pageCount)
                .originalWidth(itemReq.getOriginalWidth())
                .originalHeight(itemReq.getOriginalHeight())
                .paperSize(paperSize)
                .colorMode(colorMode)
                .orientation(itemReq.getOrientation() != null ? itemReq.getOrientation() : "AUTO")
                .copies(copies)
                .unitPrice(unitPrice)
                .totalPrice(itemTotal)
                .cropData(itemReq.getCropData())
                .build();

            orderItemRepository.save(item);
            order.getItems().add(item);
        }

        order.setSubtotal(subtotal);
        order.setTotalAmount(subtotal);
        order = orderRepository.save(order);

        if (req.getPaymentMethod() == PaymentMethod.CASH) {
            notifyNewCashOrder(order);
        }

        auditLogService.log(user != null ? user.getId().toString() : "GUEST", "USER", "ORDER_CREATED", "Order", orderNumber, "INFO",
            "Order created: " + orderNumber + " amount: " + subtotal);

        log.info("[Order] Created {} amount: {}", orderNumber, subtotal);
        return order;
    }

    @Transactional
    public Order markPaid(String orderNumber, String paymentReference, String gatewayOrderId,
                          String gatewayPaymentId, String gatewayProvider) {
        Order order = findByOrderNumber(orderNumber);
        if (order.getPaymentStatus() == PaymentStatus.PAID) return order;

        order.setPaymentStatus(PaymentStatus.PAID);
        order.setStatus(OrderStatus.PAID);
        order.setPaymentReference(paymentReference);
        order.setPaymentVerifiedAt(Instant.now());
        order.setGatewayOrderId(gatewayOrderId);
        order.setGatewayPaymentId(gatewayPaymentId);
        order.setGatewayProvider(gatewayProvider);
        order = orderRepository.save(order);

        printJobService.createPrintJobs(order);
        order.setStatus(OrderStatus.PRINT_QUEUED);
        order = orderRepository.save(order);

        log.info("[Order] {} marked PAID → PRINT_QUEUED", orderNumber);
        return order;
    }

    @Transactional
    public Order verifyCash(String orderNumber, User admin) {
        Order order = findByOrderNumber(orderNumber);
        if (order.getPaymentMethod() != PaymentMethod.CASH) {
            throw new IllegalStateException("Order is not a cash order");
        }
        return markPaid(orderNumber, "CASH-MANUAL-" + admin.getId(), null, null, "CASH");
    }

    @Transactional
    public Order cancelOrder(String orderNumber, User actor) {
        Order order = findByOrderNumber(orderNumber);
        order.setStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus(PaymentStatus.REFUNDED);
        auditLogService.log(actor != null ? actor.getId().toString() : "SYSTEM", "ADMIN", "ORDER_CANCELLED",
            "Order", orderNumber, "INFO", "Order cancelled");
        return orderRepository.save(order);
    }

    public Order findByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
            .orElseThrow(() -> new RuntimeException("Order not found: " + orderNumber));
    }

    private void notifyNewCashOrder(Order order) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "NEW_ORDER_CASH");
        payload.put("title", "New Cash Order — ISAI PRINTS");
        payload.put("body", "Order " + order.getOrderNumber() + ": ₹" + order.getTotalAmount() + " cash at counter");
        payload.put("orderNumber", order.getOrderNumber());
        payload.put("amount", order.getTotalAmount());
        payload.put("timestamp", Instant.now().toString());
        notificationService.notifyAdmin(payload);
    }

    private String generateOrderNumber() {
        String date = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC).format(Instant.now());
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "PRT-" + date + "-" + random;
    }
}
