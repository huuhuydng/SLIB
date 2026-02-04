package slib.com.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import slib.com.example.entity.users.ImportJob;
import slib.com.example.entity.users.UserImportStaging;
import slib.com.example.repository.ImportJobRepository;
import slib.com.example.repository.UserImportStagingRepository;
import slib.com.example.repository.UserRepository;
import slib.com.example.service.chat.CloudinaryService;

import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Async Import Service implementing:
 * 1. Rich Progress Pattern - immediate response, background processing
 * 2. SAX Parser (Streaming Excel) - low memory usage
 * 3. Async Avatar Enrichment - background avatar uploads
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncImportService {

    private final ImportJobRepository jobRepository;
    private final UserImportStagingRepository stagingRepository;
    private final UserRepository userRepository;
    private final StagingImportService stagingService;
    private final CloudinaryService cloudinaryService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_FORMATTER_ALT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * PHASE 1: Create job and parse Excel using SAX streaming (low memory)
     * Returns immediately with batch ID for tracking.
     */
    @Transactional
    public UUID startImport(MultipartFile file) throws Exception {
        UUID batchId = UUID.randomUUID();

        // Create import job
        ImportJob job = ImportJob.builder()
                .batchId(batchId)
                .fileName(file.getOriginalFilename())
                .status(ImportJob.ImportJobStatus.PARSING)
                .build();
        jobRepository.save(job);

        log.info("[AsyncImport] Starting import job: {} for file: {}", batchId, file.getOriginalFilename());

        // Parse Excel using SAX streaming
        int rowCount = parseExcelStreaming(file.getInputStream(), batchId);

        // Update total rows
        job.setTotalRows(rowCount);
        jobRepository.save(job);

        log.info("[AsyncImport] Parsed {} rows to staging", rowCount);

        return batchId;
    }

    /**
     * PHASE 2: Process import asynchronously (validation + import)
     * Called after startImport, runs in background.
     */
    @Async("importExecutor")
    public CompletableFuture<Void> processImportAsync(UUID batchId) {
        try {
            log.info("[AsyncImport] Starting async processing for batch: {}", batchId);

            // Step 1: Validate using SQL (database-level)
            stagingService.validateStagingData(batchId);

            // Step 2: Import valid users with parallel BCrypt
            int imported = stagingService.importValidUsers(batchId);

            log.info("[AsyncImport] Imported {} users for batch: {}", imported, batchId);

            // Mark as completed (avatars will be enriched separately)
            stagingService.completeJob(batchId);

            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("[AsyncImport] Failed processing batch: {}", batchId, e);
            stagingService.failJob(batchId, e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * PHASE 3: Enrich avatars asynchronously (background upload)
     * This runs AFTER import is complete, so users see "success" immediately.
     * Progress is tracked as "uploaded/total" format.
     */
    @Async("avatarExecutor")
    public CompletableFuture<Void> enrichAvatarsAsync(UUID batchId, Map<String, MultipartFile> avatarFiles) {
        if (avatarFiles == null || avatarFiles.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        try {
            int total = avatarFiles.size();
            log.info("[AsyncImport] Starting avatar enrichment: 0/{}", total);

            // Update job status and set avatar count
            jobRepository.updateStatus(batchId, ImportJob.ImportJobStatus.ENRICHING);
            jobRepository.updateAvatarCount(batchId, total);

            AtomicInteger uploaded = new AtomicInteger(0);
            AtomicInteger failed = new AtomicInteger(0);

            // Upload avatars in parallel
            avatarFiles.entrySet().parallelStream().forEach(entry -> {
                String userCode = entry.getKey();
                MultipartFile file = entry.getValue();

                try {
                    String avatarUrl = cloudinaryService.uploadAvatar(file);

                    // Update user's avatar URL in database
                    userRepository.updateAvatarUrl(userCode, avatarUrl);

                    int count = uploaded.incrementAndGet();

                    // Log progress every avatar
                    log.info("[Avatar] Progress: {}/{} - {} OK", count, total, userCode);

                    // Update DB every 5 uploads to reduce DB calls
                    if (count % 5 == 0 || count == total) {
                        jobRepository.updateAvatarUploaded(batchId, count);
                    }

                } catch (Exception e) {
                    int failCount = failed.incrementAndGet();
                    log.warn("[Avatar] Failed: {}/{} - {} - {}",
                            uploaded.get(), total, userCode, e.getMessage());
                }
            });

            // Final update
            int successCount = uploaded.get();
            int failCount = failed.get();
            jobRepository.updateAvatarUploaded(batchId, successCount);

            log.info("[AsyncImport] Avatar enrichment complete: {}/{} (failed: {})",
                    successCount, total, failCount);

            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("[AsyncImport] Avatar enrichment failed: {}", e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * SAX Streaming Excel Parser - reads row by row, very low memory usage
     * Can handle millions of rows with <50MB RAM
     */
    private int parseExcelStreaming(InputStream inputStream, UUID batchId) throws Exception {
        try (OPCPackage pkg = OPCPackage.open(inputStream)) {
            XSSFReader reader = new XSSFReader(pkg);
            ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(pkg);
            StylesTable styles = reader.getStylesTable();

            // Get first sheet
            XSSFReader.SheetIterator sheets = (XSSFReader.SheetIterator) reader.getSheetsData();
            if (!sheets.hasNext()) {
                throw new RuntimeException("Excel file has no sheets");
            }

            List<UserImportStaging> batch = new ArrayList<>();
            AtomicInteger rowNum = new AtomicInteger(0);
            AtomicInteger dataRowNum = new AtomicInteger(0);

            // Custom content handler for SAX parsing
            XSSFSheetXMLHandler.SheetContentsHandler handler = new XSSFSheetXMLHandler.SheetContentsHandler() {
                private Map<String, String> currentRow = new HashMap<>();
                private boolean isHeaderRow = true;

                @Override
                public void startRow(int row) {
                    currentRow.clear();
                }

                @Override
                public void endRow(int row) {
                    rowNum.incrementAndGet();

                    if (isHeaderRow) {
                        // First row is header, skip it
                        isHeaderRow = false;
                        return;
                    }

                    // Skip empty rows
                    if (currentRow.isEmpty())
                        return;

                    // Create staging entry
                    UserImportStaging staging = UserImportStaging.builder()
                            .batchId(batchId)
                            .rowNumber(dataRowNum.incrementAndGet())
                            .userCode(getCell("A"))
                            .fullName(getCell("B"))
                            .email(getCell("C"))
                            .phone(getCell("D"))
                            .dob(parseDate(getCell("E")))
                            .role(getCell("F"))
                            .status(UserImportStaging.StagingStatus.PENDING)
                            .build();

                    batch.add(staging);

                    // Flush batch every 500 rows for memory efficiency
                    if (batch.size() >= 500) {
                        stagingRepository.saveAll(new ArrayList<>(batch));
                        batch.clear();
                    }
                }

                @Override
                public void cell(String cellRef, String value, XSSFComment comment) {
                    // Extract column letter (A, B, C, ...)
                    String col = cellRef.replaceAll("[0-9]", "");
                    currentRow.put(col, value);
                }

                private String getCell(String col) {
                    return currentRow.getOrDefault(col, "");
                }

                private LocalDate parseDate(String dateStr) {
                    if (dateStr == null || dateStr.isEmpty())
                        return null;
                    try {
                        return LocalDate.parse(dateStr, DATE_FORMATTER);
                    } catch (Exception e1) {
                        try {
                            return LocalDate.parse(dateStr, DATE_FORMATTER_ALT);
                        } catch (Exception e2) {
                            return null;
                        }
                    }
                }
            };

            // Parse using SAX (using SAXParserFactory instead of deprecated
            // XMLReaderFactory)
            try (InputStream sheetStream = sheets.next()) {
                SAXParserFactory factory = SAXParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XMLReader parser = factory.newSAXParser().getXMLReader();
                parser.setContentHandler(new XSSFSheetXMLHandler(styles, strings, handler, false));
                parser.parse(new InputSource(sheetStream));
            }

            // Save remaining batch
            if (!batch.isEmpty()) {
                stagingRepository.saveAll(batch);
            }

            return dataRowNum.get();
        }
    }
}
