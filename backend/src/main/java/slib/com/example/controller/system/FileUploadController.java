package slib.com.example.controller.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import slib.com.example.service.chat.CloudinaryService;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/slib/files")
public class FileUploadController {

    // File size limits (in bytes)
    private static final long MAX_CHAT_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final long MAX_NEWS_IMAGE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final long MAX_DOCUMENT_SIZE = 20 * 1024 * 1024; // 20MB

    @Autowired
    private CloudinaryService cloudinaryService;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/upload_news_image")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // Validate file size
            if (file.getSize() > MAX_NEWS_IMAGE_SIZE) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "File quá lớn. Tối đa cho ảnh tin tức: 10MB",
                        "maxSize", "10MB",
                        "actualSize", formatFileSize(file.getSize())));
            }

            String imageUrl = cloudinaryService.uploadImageNews(file);
            return ResponseEntity.ok().body(Map.of("url", imageUrl, "type", "IMAGE"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi upload News: " + e.getMessage()));
        }
    }

    @PostMapping("/upload_news_pdf")
    public ResponseEntity<?> uploadNewsPdf(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File PDF không được để trống"));
            }
            if (file.getSize() > MAX_DOCUMENT_SIZE) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "File quá lớn. Tối đa cho PDF tin tức: 20MB",
                        "maxSize", "20MB",
                        "actualSize", formatFileSize(file.getSize())));
            }

            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "news-attachment.pdf";
            String contentType = file.getContentType() != null ? file.getContentType().toLowerCase() : "";
            if (!originalName.toLowerCase().endsWith(".pdf") || !"application/pdf".equals(contentType)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Chỉ chấp nhận file PDF hợp lệ"));
            }

            String fileUrl = cloudinaryService.uploadNewsPdf(file);
            return ResponseEntity.ok().body(Map.of(
                    "url", fileUrl,
                    "type", "PDF",
                    "fileName", originalName,
                    "fileSize", file.getSize(),
                    "mimeType", "application/pdf"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi upload PDF tin tức: " + e.getMessage()));
        }
    }

    @PostMapping("/upload_chat_image")
    public ResponseEntity<?> uploadChatImage(@RequestParam("file") MultipartFile file) {
        try {
            // Validate file size - strict limit for student uploads
            if (file.getSize() > MAX_CHAT_IMAGE_SIZE) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "File quá lớn. Tối đa cho chat: 5MB",
                        "maxSize", "5MB",
                        "actualSize", formatFileSize(file.getSize())));
            }

            String imageUrl = cloudinaryService.uploadImageChat(file);
            return ResponseEntity.ok().body(Map.of("url", imageUrl, "type", "IMAGE"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi upload Chat: " + e.getMessage()));
        }
    }

    @PostMapping("/upload_document")
    public ResponseEntity<?> uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            // Validate file size
            if (file.getSize() > MAX_DOCUMENT_SIZE) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "File quá lớn. Tối đa cho tài liệu: 20MB",
                        "maxSize", "20MB",
                        "actualSize", formatFileSize(file.getSize())));
            }

            String fileUrl = cloudinaryService.uploadDocument(file);
            return ResponseEntity.ok().body(Map.of("url", fileUrl, "type", "FILE"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi upload tài liệu: " + e.getMessage()));
        }
    }

    @GetMapping("/proxy-image")
    public ResponseEntity<?> proxyImage(@RequestParam("url") String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();

            if (host == null || !host.equals("res.cloudinary.com")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Chỉ hỗ trợ proxy ảnh từ Cloudinary"));
            }

            ResponseEntity<byte[]> upstream = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    null,
                    byte[].class);

            byte[] body = upstream.getBody();
            if (body == null || body.length == 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Không thể tải ảnh từ Cloudinary"));
            }

            MediaType mediaType = upstream.getHeaders().getContentType();
            if (mediaType == null) {
                mediaType = MediaType.IMAGE_JPEG;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(mediaType);
            headers.setCacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic());

            return ResponseEntity
                    .status(upstream.getStatusCode())
                    .headers(headers)
                    .body(body);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Không thể proxy ảnh: " + e.getMessage()));
        }
    }

    @GetMapping("/proxy-pdf")
    public ResponseEntity<?> proxyPdf(
            @RequestParam("url") String url,
            @RequestParam(value = "fileName", required = false) String fileName,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();

            if (host == null || !host.equals("res.cloudinary.com")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Chỉ hỗ trợ proxy PDF từ Cloudinary"));
            }

            String path = uri.getPath() != null ? uri.getPath().toLowerCase() : "";
            if (!path.contains("/upload/") || (!path.endsWith(".pdf") && !path.contains("/slib_news_pdfs/"))) {
                return ResponseEntity.badRequest().body(Map.of("error", "Đường dẫn không phải file PDF hợp lệ"));
            }

            HttpHeaders upstreamHeaders = new HttpHeaders();
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                upstreamHeaders.set(HttpHeaders.RANGE, rangeHeader);
            }

            ResponseEntity<byte[]> upstream = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    new HttpEntity<>(upstreamHeaders),
                    byte[].class);

            byte[] body = upstream.getBody();
            if (body == null || body.length == 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Không thể tải PDF từ Cloudinary"));
            }

            String safeFileName = sanitizeFileName(fileName);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.inline()
                    .filename(safeFileName, StandardCharsets.UTF_8)
                    .build());
            headers.setCacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic());
            headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
            String contentRange = upstream.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE);
            if (contentRange != null) {
                headers.set(HttpHeaders.CONTENT_RANGE, contentRange);
            }
            headers.setContentLength(body.length);

            return ResponseEntity
                    .status(upstream.getStatusCode())
                    .headers(headers)
                    .body(body);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Không thể proxy PDF: " + e.getMessage()));
        }
    }

    /**
     * Format file size to human readable string
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        if (bytes < 1024 * 1024)
            return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "slib-news.pdf";
        }
        String sanitized = fileName.replaceAll("[\\\\/\\r\\n\"]", "_").trim();
        if (!sanitized.toLowerCase().endsWith(".pdf")) {
            sanitized += ".pdf";
        }
        return sanitized;
    }
}
