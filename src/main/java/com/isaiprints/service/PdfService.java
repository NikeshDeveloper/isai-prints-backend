package com.isaiprints.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;
import java.io.IOException;

@Service
@Slf4j
public class PdfService {
    public int getPageCount(byte[] pdfBytes) {
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            int count = doc.getNumberOfPages();
            log.info("[PDF] Page count: {}", count);
            return Math.max(1, count);
        } catch (IOException e) {
            log.error("[PDF] Failed to parse PDF for page count: {}", e.getMessage());
            return 1;
        }
    }

    public boolean isPdf(byte[] bytes) {
        return bytes != null && bytes.length >= 4
            && bytes[0] == 0x25 && bytes[1] == 0x50
            && bytes[2] == 0x44 && bytes[3] == 0x46;
    }
}
