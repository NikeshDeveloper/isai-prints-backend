package com.isaiprints.controller;

import com.isaiprints.domain.entity.FileAsset;
import com.isaiprints.domain.entity.User;
import com.isaiprints.domain.repository.FileAssetRepository;
import com.isaiprints.service.PdfService;
import com.isaiprints.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class UploadController {
    private final StorageService storageService;
    private final PdfService pdfService;
    private final FileAssetRepository fileAssetRepository;

    private static final long MAX_SIZE = 25 * 1024 * 1024L;
    private static final java.util.Set<String> ALLOWED_MIME = java.util.Set.of(
        "image/jpeg", "image/jpg", "image/png", "image/webp", "application/pdf"
    );

    @PostMapping("/uploads")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user) {
        try {
            if (file.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Empty file"));
            if (file.getSize() > MAX_SIZE) return ResponseEntity.badRequest().body(Map.of("error", "File too large (max 25MB)"));

            byte[] bytes = file.getBytes();
            String mimeType = detectMimeType(bytes, file.getOriginalFilename());
            if (!ALLOWED_MIME.contains(mimeType)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Unsupported file type. Allowed: JPG, PNG, WEBP, PDF"));
            }

            boolean isPdf = mimeType.equals("application/pdf");
            String ext = isPdf ? ".pdf" : "." + mimeType.split("/")[1];
            String key = (isPdf ? "pdfs/" : "images/") + UUID.randomUUID() + ext;
            storageService.upload(bytes, key, mimeType);

            int pageCount = isPdf ? pdfService.getPageCount(bytes) : 1;
            String fileType = isPdf ? "PDF" : "IMAGE";

            FileAsset asset = FileAsset.builder()
                .storageKey(key)
                .originalFilename(file.getOriginalFilename())
                .mimeType(mimeType)
                .fileType(fileType)
                .fileSize(file.getSize())
                .pageCount(pageCount)
                .uploadedBy(user)
                .build();
            fileAssetRepository.save(asset);

            log.info("[Upload] {} ({}) -> {} pages", file.getOriginalFilename(), mimeType, pageCount);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "storageKey", key,
                "originalFilename", file.getOriginalFilename(),
                "mimeType", mimeType,
                "fileType", fileType,
                "fileSize", file.getSize(),
                "pageCount", pageCount,
                "previewUrl", "/api/files/" + key
            ));
        } catch (Exception e) {
            log.error("Upload error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }

    private String detectMimeType(byte[] bytes, String filename) {
        if (bytes.length >= 4 && bytes[0] == 0x25 && bytes[1] == 0x50 && bytes[2] == 0x44 && bytes[3] == 0x46) return "application/pdf";
        if (bytes.length >= 3 && bytes[0] == (byte)0xFF && bytes[1] == (byte)0xD8 && bytes[2] == (byte)0xFF) return "image/jpeg";
        if (bytes.length >= 8 && bytes[0] == (byte)0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) return "image/png";
        if (bytes.length >= 12 && bytes[0] == 0x52 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x46 && bytes[8] == 0x57 && bytes[9] == 0x45 && bytes[10] == 0x42 && bytes[11] == 0x50) return "image/webp";
        if (filename != null && filename.toLowerCase().endsWith(".pdf")) return "application/pdf";
        return "application/octet-stream";
    }
}
