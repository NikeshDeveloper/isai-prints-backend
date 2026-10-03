package com.isaiprints.service;

import com.isaiprints.domain.entity.*;
import com.isaiprints.domain.enums.PrintJobStatus;
import com.isaiprints.domain.repository.OrderRepository;
import com.isaiprints.domain.repository.PrintJobRepository;
import com.isaiprints.exception.ConflictException;
import com.isaiprints.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrintJobService {
    private final PrintJobRepository printJobRepository;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public PrintJob findById(UUID id) {
        return printJobRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("PrintJob", id.toString()));
    }

    @Transactional
    public void createPrintJobs(Order order) {
        for (OrderItem item : order.getItems()) {
            PrintJob job = PrintJob.builder()
                .order(order)
                .orderItem(item)
                .orderNumber(order.getOrderNumber())
                .status(PrintJobStatus.QUEUED)
                .build();
            printJobRepository.save(job);
        }
        log.info("[PrintJob] Created {} jobs for order {}", order.getItems().size(), order.getOrderNumber());
    }

    public List<PrintJob> getPendingJobs() {
        return printJobRepository.findByStatus(PrintJobStatus.QUEUED);
    }

    @Transactional
    public PrintJob claimJob(UUID jobId, String agentId, String printerName) {
        PrintJob job = printJobRepository.findByIdAndStatusForUpdate(jobId, PrintJobStatus.QUEUED)
            .orElseThrow(() -> new ConflictException("Job " + jobId + " is not available for claiming"));

        job.setStatus(PrintJobStatus.CLAIMED);
        job.setAgentId(agentId);
        job.setPrinterName(printerName);
        job.setClaimedAt(Instant.now());
        job.setClaimToken(UUID.randomUUID().toString());
        job.setAttemptCount(job.getAttemptCount() + 1);
        PrintJob saved = printJobRepository.save(job);
        log.info("[PrintJob] Job {} claimed by agent {}", jobId, agentId);
        return saved;
    }

    @Transactional
    public PrintJob startJob(UUID jobId, String agentId) {
        PrintJob job = findById(jobId);
        job.setStatus(PrintJobStatus.PRINTING);
        job.setStartedAt(Instant.now());
        
        Order order = job.getOrder();
        order.setStatus(com.isaiprints.domain.enums.OrderStatus.PRINTING);
        orderRepository.save(order);
        
        return printJobRepository.save(job);
    }

    @Transactional
    public PrintJob completeJob(UUID jobId, String agentId) {
        PrintJob job = findById(jobId);
        job.setStatus(PrintJobStatus.COMPLETED);
        job.setCompletedAt(Instant.now());
        PrintJob saved = printJobRepository.save(job);
        
        Order orderFresh = orderRepository.findById(job.getOrder().getId()).orElse(job.getOrder());
        boolean allDone = orderFresh.getPrintJobs().stream()
            .allMatch(j -> j.getStatus() == PrintJobStatus.COMPLETED || j.getStatus() == PrintJobStatus.FAILED);
        if (allDone) {
            orderFresh.setStatus(com.isaiprints.domain.enums.OrderStatus.COMPLETED);
            orderRepository.save(orderFresh);
        }
        
        Map<String, Object> payload = Map.of(
            "type", "PRINT_COMPLETED",
            "title", "Print Complete — ISAI PRINTS",
            "body", "Job " + job.getOrderNumber() + " printed by " + agentId,
            "orderNumber", job.getOrderNumber()
        );
        notificationService.notifyAdmin(payload);
        log.info("[PrintJob] Job {} COMPLETED by agent {}", jobId, agentId);
        return saved;
    }

    @Transactional
    public PrintJob failJob(UUID jobId, String agentId, String errorMessage) {
        PrintJob job = findById(jobId);
        job.setStatus(PrintJobStatus.FAILED);
        job.setErrorMessage(errorMessage);
        PrintJob saved = printJobRepository.save(job);
        
        Map<String, Object> payload = Map.of(
            "type", "PRINT_FAILED",
            "title", "Print Failed — ISAI PRINTS",
            "body", "Job " + job.getOrderNumber() + " failed: " + errorMessage,
            "orderNumber", job.getOrderNumber()
        );
        notificationService.notifyAdmin(payload);
        auditLogService.log(agentId, "AGENT", "JOB_FAILED", "PrintJob", jobId.toString(), "ERROR", errorMessage);
        return saved;
    }
}
