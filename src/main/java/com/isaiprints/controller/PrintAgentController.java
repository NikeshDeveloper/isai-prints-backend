package com.isaiprints.controller;

import com.isaiprints.domain.entity.PrintJob;
import com.isaiprints.dto.request.ClaimJobRequest;
import com.isaiprints.service.PrintJobService;
import com.isaiprints.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/print-agent")
@RequiredArgsConstructor
@Slf4j
public class PrintAgentController {
    private final PrintJobService printJobService;
    private final StorageService storageService;

    @GetMapping("/jobs")
    @PreAuthorize("hasRole('PRINT_AGENT')")
    public ResponseEntity<Map<String, Object>> getPendingJobs() {
        List<PrintJob> jobs = printJobService.getPendingJobs();
        List<Map<String, Object>> jobList = jobs.stream().map(job -> {
            String storageKey = job.getOrderItem().getStorageKey();
            String downloadUrl = (storageKey != null && !storageKey.isBlank())
                ? storageService.getDownloadUrl(storageKey)
                : null;
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", job.getId().toString());
            map.put("orderNumber", job.getOrderNumber());
            map.put("copies", job.getOrderItem().getCopies());
            map.put("paperSize", job.getOrderItem().getPaperSize().name());
            map.put("colorMode", job.getOrderItem().getColorMode().name());
            map.put("orientation", job.getOrderItem().getOrientation());
            map.put("fileType", job.getOrderItem().getFileType() != null ? job.getOrderItem().getFileType() : "IMAGE");
            map.put("mimeType", job.getOrderItem().getMimeType() != null ? job.getOrderItem().getMimeType() : "application/octet-stream");
            map.put("pageCount", job.getOrderItem().getPageCount() != null ? job.getOrderItem().getPageCount() : 1);
            map.put("downloadUrl", downloadUrl != null ? downloadUrl : "");
            map.put("originalFilename", job.getOrderItem().getOriginalFilename());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("jobs", jobList));
    }

    @PostMapping("/jobs/{id}/claim")
    @PreAuthorize("hasRole('PRINT_AGENT')")
    public ResponseEntity<Map<String, Object>> claimJob(@PathVariable UUID id,
                                                         @RequestBody ClaimJobRequest req) {
        PrintJob job = printJobService.claimJob(id, req.getAgentId(), req.getPrinterName());
        return ResponseEntity.ok(Map.of(
            "success", true,
            "job", Map.of(
                "id", job.getId().toString(),
                "status", job.getStatus().name(),
                "claimToken", job.getClaimToken() != null ? job.getClaimToken() : ""
            )
        ));
    }

    @PostMapping("/jobs/{id}/start")
    @PreAuthorize("hasRole('PRINT_AGENT')")
    public ResponseEntity<Map<String, Object>> startJob(@PathVariable UUID id,
                                                         @RequestBody Map<String, String> body) {
        printJobService.startJob(id, body.get("agent_id"));
        return ResponseEntity.ok(Map.of("success", true, "status", "PRINTING"));
    }

    @GetMapping("/jobs/{id}/file")
    @PreAuthorize("hasRole('PRINT_AGENT')")
    public ResponseEntity<byte[]> downloadJobFile(@PathVariable UUID id) {
        try {
            PrintJob job = printJobService.findById(id);
            byte[] fileBytes = storageService.download(job.getOrderItem().getStorageKey());
            String mimeType = job.getOrderItem().getMimeType();
            return ResponseEntity.ok()
                .header("Content-Type", mimeType)
                .header("Content-Disposition", "attachment; filename=\"" + job.getOrderItem().getOriginalFilename() + "\"")
                .body(fileBytes);
        } catch (Exception e) {
            log.error("File download error for job {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/jobs/{id}/complete")
    @PreAuthorize("hasRole('PRINT_AGENT')")
    public ResponseEntity<Map<String, Object>> completeJob(@PathVariable UUID id,
                                                            @RequestBody Map<String, String> body) {
        printJobService.completeJob(id, body.get("agent_id"));
        return ResponseEntity.ok(Map.of("success", true, "status", "COMPLETED"));
    }

    @PostMapping("/jobs/{id}/fail")
    @PreAuthorize("hasRole('PRINT_AGENT')")
    public ResponseEntity<Map<String, Object>> failJob(@PathVariable UUID id,
                                                        @RequestBody Map<String, String> body) {
        printJobService.failJob(id, body.get("agent_id"), body.getOrDefault("error_message", "Unknown error"));
        return ResponseEntity.ok(Map.of("success", true, "status", "FAILED"));
    }
}
