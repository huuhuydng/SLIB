package slib.com.example.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
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
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Async Import Service implementing:
 * 1. Rich Progress Pattern - immediate response, background processing
 * 2. SAX Parser (Streaming Excel) - low memory usage
 * 3. Server-side ZIP processing - extract Excel + avatars in one upload
 * 4. ExecutorService (20 threads) for parallel avatar uploads (I/O-bound)
 */
@Slf4j
@Service
public class AsyncImportService {

    private final ImportJobRepository jobRepository;
    private final UserImportStagingRepository stagingRepository;
    private final UserRepository userRepository;
    private final StagingImportService stagingService;
    private final CloudinaryService cloudinaryService;
    private final TransactionTemplate transactionTemplate;

    public AsyncImportService(
            ImportJobRepository jobRepository,
            UserImportStagingRepository stagingRepository,
            UserRepository userRepository,
            StagingImportService stagingService,
            CloudinaryService cloudinaryService,
            PlatformTransactionManager transactionManager) {
        this.jobRepository = jobRepository;
        this.stagingRepository = stagingRepository;
        this.userRepository = userRepository;
        this.stagingService = stagingService;
        this.cloudinaryService = cloudinaryService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_FORMATTER_ALT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Avatar upload concurrency (I/O-bound, not CPU-bound)
    private static final int AVATAR_UPLOAD_THREADS = 20;

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
     * ZIP IMPORT: Parse ZIP file server-side (Excel + avatars)
     * Returns batchId + avatarMap (extract once, avoid re-reading ZIP)
     */
    @Transactional
    public AbstractMap.SimpleEntry<UUID, Map<String, byte[]>> startZipImport(MultipartFile zipFile) throws Exception {
        UUID batchId = UUID.randomUUID();

        // Create import job
        ImportJob job = ImportJob.builder()
                .batchId(batchId)
                .fileName(zipFile.getOriginalFilename())
                .status(ImportJob.ImportJobStatus.PARSING)
                .build();
        jobRepository.save(job);

        log.info("[ZipImport] Starting ZIP import: {} (size: {}MB)", zipFile.getOriginalFilename(),
                zipFile.getSize() / (1024 * 1024));

        // Extract ZIP: find Excel file + avatar images (single pass)
        byte[] excelBytes = null;
        Map<String, byte[]> avatarMap = new LinkedHashMap<>();

        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();

                // Skip directories, Mac metadata files
                if (entry.isDirectory() || name.startsWith("__MACOSX") || name.startsWith("._")) {
                    continue;
                }

                // Get just the filename without path
                String fileName = name.contains("/") ? name.substring(name.lastIndexOf('/') + 1) : name;

                if (fileName.startsWith("._") || fileName.startsWith(".")) {
                    continue;
                }

                byte[] data = zis.readAllBytes();

                if (fileName.toLowerCase().endsWith(".xlsx") || fileName.toLowerCase().endsWith(".xls")) {
                    excelBytes = data;
                    log.info("[ZipImport] Found Excel: {} ({}KB)", fileName, data.length / 1024);
                } else if (isImageFile(fileName)) {
                    // Extract userCode from filename (e.g., SL000001.png -> SL000001)
                    String userCode = fileName.substring(0, fileName.lastIndexOf('.')).toUpperCase();
                    avatarMap.put(userCode, data);
                }
            }
        }

        if (excelBytes == null) {
            jobRepository.updateStatus(batchId, ImportJob.ImportJobStatus.FAILED);
            throw new RuntimeException("Không tìm thấy file Excel (.xlsx/.xls) trong ZIP");
        }

        log.info("[ZipImport] Extracted: 1 Excel + {} avatars", avatarMap.size());

        // Parse Excel using SAX streaming
        int rowCount = parseExcelStreaming(new ByteArrayInputStream(excelBytes), batchId);
        job.setTotalRows(rowCount);
        job.setAvatarCount(avatarMap.size());
        jobRepository.save(job);

        log.info("[ZipImport] Parsed {} rows to staging, {} avatars ready", rowCount, avatarMap.size());

        return new AbstractMap.SimpleEntry<>(batchId, avatarMap);
    }

    /**
     * Process ZIP import: validate + import + upload avatars (all async)
     */
    @Async("importExecutor")
    public CompletableFuture<Void> processZipImportAsync(UUID batchId, Map<String, byte[]> avatarMap) {
        try {
            log.info("[ZipImport] Starting async processing for batch: {}", batchId);

            // Step 1: Validate using SQL
            stagingService.validateStagingData(batchId);

            // Step 2: Import valid users
            int imported = stagingService.importValidUsers(batchId);
            log.info("[ZipImport] Imported {} users for batch: {}", imported, batchId);

            // Step 3: Upload avatars with high-concurrency ExecutorService
            if (avatarMap != null && !avatarMap.isEmpty()) {
                uploadAvatarsWithExecutor(batchId, avatarMap);
            }

            // Step 4: Complete
            stagingService.completeJob(batchId);
            log.info("[ZipImport] Completed batch: {}", batchId);

            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("[ZipImport] Failed processing batch: {}", batchId, e);
            stagingService.failJob(batchId, e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Upload avatars using dedicated ExecutorService with 20 threads
     * Much faster than parallelStream (ForkJoinPool limited to CPU cores)
     * for I/O-bound Cloudinary uploads
     */
    private void uploadAvatarsWithExecutor(UUID batchId, Map<String, byte[]> avatarMap) {
        int total = avatarMap.size();
        log.info("[Avatar] Starting upload: {} avatars with {} threads", total, AVATAR_UPLOAD_THREADS);

        transactionTemplate.executeWithoutResult(status -> {
            jobRepository.updateStatus(batchId, ImportJob.ImportJobStatus.ENRICHING);
            jobRepository.updateAvatarCount(batchId, total);
        });

        AtomicInteger uploaded = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        List<String> uploadedUrls = Collections.synchronizedList(new ArrayList<>());

        ExecutorService executor = Executors.newFixedThreadPool(AVATAR_UPLOAD_THREADS);

        try {
            List<CompletableFuture<Void>> futures = avatarMap.entrySet().stream()
                    .map(entry -> CompletableFuture.runAsync(() -> {
                        String userCode = entry.getKey();
                        byte[] imageBytes = entry.getValue();

                        try {
                            String avatarUrl = cloudinaryService.uploadAvatarBytes(imageBytes, userCode);
                            uploadedUrls.add(avatarUrl);

                            // Update user's avatar URL in database
                            transactionTemplate.executeWithoutResult(
                                    status -> userRepository.updateAvatarUrl(userCode, avatarUrl));

                            int count = uploaded.incrementAndGet();

                            // Update progress every 10 uploads
                            if (count % 10 == 0 || count == total) {
                                transactionTemplate.executeWithoutResult(
                                        status -> jobRepository.updateAvatarUploaded(batchId, count));
                                log.info("[Avatar] Progress: {}/{} ({} failed)", count, total, failed.get());
                            }

                        } catch (Exception e) {
                            failed.incrementAndGet();
                            log.warn("[Avatar] Failed: {} - {}", userCode, e.getMessage());
                        }
                    }, executor))
                    .toList();

            // Wait for all uploads to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        } finally {
            executor.shutdown();
            try {
                executor.awaitTermination(5, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Final update
        transactionTemplate.executeWithoutResult(status -> jobRepository.updateAvatarUploaded(batchId, uploaded.get()));

        int successCount = uploaded.get();
        int failCount = failed.get();
        log.info("[Avatar] Upload complete: {}/{} success, {} failed", successCount, total, failCount);

        // Rollback if ALL uploads failed
        if (successCount == 0 && failCount > 0) {
            log.error("[Avatar] ALL uploads failed! Rolling back...");
            rollbackCloudinaryUploads(uploadedUrls);
            throw new RuntimeException("Toàn bộ upload avatar thất bại");
        }
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
     * Uses ExecutorService with 20 threads for I/O-bound uploads
     */
    @Async("avatarExecutor")
    public CompletableFuture<Void> enrichAvatarsAsync(UUID batchId, Map<String, MultipartFile> avatarFiles) {
        if (avatarFiles == null || avatarFiles.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        List<String> uploadedUrls = Collections.synchronizedList(new ArrayList<>());

        try {
            int total = avatarFiles.size();
            log.info("[AsyncImport] Starting avatar enrichment: 0/{}", total);

            jobRepository.updateStatus(batchId, ImportJob.ImportJobStatus.ENRICHING);
            jobRepository.updateAvatarCount(batchId, total);

            AtomicInteger uploaded = new AtomicInteger(0);
            AtomicInteger failed = new AtomicInteger(0);

            // Use ExecutorService instead of parallelStream for better I/O concurrency
            ExecutorService executor = Executors.newFixedThreadPool(AVATAR_UPLOAD_THREADS);

            try {
                List<CompletableFuture<Void>> futures = avatarFiles.entrySet().stream()
                        .map(entry -> CompletableFuture.runAsync(() -> {
                            String userCode = entry.getKey();
                            MultipartFile file = entry.getValue();

                            try {
                                String avatarUrl = cloudinaryService.uploadAvatar(file);
                                uploadedUrls.add(avatarUrl);
                                userRepository.updateAvatarUrl(userCode, avatarUrl);

                                int count = uploaded.incrementAndGet();
                                if (count % 10 == 0 || count == total) {
                                    jobRepository.updateAvatarUploaded(batchId, count);
                                    log.info("[Avatar] Progress: {}/{}", count, total);
                                }

                            } catch (Exception e) {
                                failed.incrementAndGet();
                                log.warn("[Avatar] Failed: {} - {}", userCode, e.getMessage());
                            }
                        }, executor))
                        .toList();

                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            } finally {
                executor.shutdown();
                executor.awaitTermination(10, TimeUnit.MINUTES);
            }

            int successCount = uploaded.get();
            int failCount = failed.get();
            jobRepository.updateAvatarUploaded(batchId, successCount);

            if (successCount == 0 && failCount > 0) {
                log.error("[AsyncImport] ALL avatar uploads failed! Rolling back...");
                rollbackCloudinaryUploads(uploadedUrls);
                jobRepository.updateStatus(batchId, ImportJob.ImportJobStatus.FAILED);
                return CompletableFuture.failedFuture(
                        new RuntimeException("Toàn bộ upload avatar thất bại. Đã rollback ảnh trên Cloudinary."));
            }

            log.info("[AsyncImport] Avatar enrichment complete: {}/{} (failed: {})",
                    successCount, total, failCount);

            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("[AsyncImport] Avatar enrichment crashed: {}. Rolling back {} uploads...",
                    e.getMessage(), uploadedUrls.size());
            rollbackCloudinaryUploads(uploadedUrls);
            jobRepository.updateStatus(batchId, ImportJob.ImportJobStatus.FAILED);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Rollback: Xóa tất cả ảnh đã upload lên Cloudinary khi import thất bại.
     */
    private void rollbackCloudinaryUploads(List<String> uploadedUrls) {
        if (uploadedUrls == null || uploadedUrls.isEmpty()) {
            return;
        }
        try {
            int deleted = cloudinaryService.deleteAvatars(uploadedUrls);
            log.info("[Rollback] Đã xóa {}/{} ảnh trên Cloudinary", deleted, uploadedUrls.size());
        } catch (Exception e) {
            log.error("[Rollback] Lỗi khi xóa ảnh Cloudinary: {}", e.getMessage());
        }
    }

    /**
     * SAX Streaming Excel Parser - reads row by row, very low memory usage
     */
    private int parseExcelStreaming(InputStream inputStream, UUID batchId) throws Exception {
        try (OPCPackage pkg = OPCPackage.open(inputStream)) {
            XSSFReader reader = new XSSFReader(pkg);
            ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(pkg);
            StylesTable styles = reader.getStylesTable();

            XSSFReader.SheetIterator sheets = (XSSFReader.SheetIterator) reader.getSheetsData();
            if (!sheets.hasNext()) {
                throw new RuntimeException("Excel file has no sheets");
            }

            List<UserImportStaging> batch = new ArrayList<>();
            AtomicInteger rowNum = new AtomicInteger(0);
            AtomicInteger dataRowNum = new AtomicInteger(0);

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
                        isHeaderRow = false;
                        return;
                    }

                    if (currentRow.isEmpty())
                        return;

                    UserImportStaging staging = UserImportStaging.builder()
                            .batchId(batchId)
                            .rowNumber(dataRowNum.incrementAndGet())
                            .fullName(getCell("A"))
                            .userCode(getCell("B"))
                            .role(getCell("C"))
                            .dob(parseDate(getCell("D")))
                            .email(getCell("E"))
                            .phone(getCell("F"))
                            .status(UserImportStaging.StagingStatus.PENDING)
                            .build();

                    batch.add(staging);

                    if (batch.size() >= 500) {
                        stagingRepository.saveAll(new ArrayList<>(batch));
                        batch.clear();
                    }
                }

                @Override
                public void cell(String cellRef, String value, XSSFComment comment) {
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

            try (InputStream sheetStream = sheets.next()) {
                SAXParserFactory factory = SAXParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XMLReader parser = factory.newSAXParser().getXMLReader();
                parser.setContentHandler(new XSSFSheetXMLHandler(styles, strings, handler, false));
                parser.parse(new InputSource(sheetStream));
            }

            if (!batch.isEmpty()) {
                stagingRepository.saveAll(batch);
            }

            return dataRowNum.get();
        }
    }

    private boolean isImageFile(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".gif") || lower.endsWith(".webp");
    }
}
