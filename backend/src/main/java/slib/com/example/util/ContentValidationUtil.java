package slib.com.example.util;

import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import slib.com.example.exception.BadRequestException;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ContentValidationUtil {
    private static final Set<String> ALLOWED_FEEDBACK_CATEGORIES = Set.of("FACILITY", "SERVICE", "GENERAL", "MESSAGE");
    private static final long DEFAULT_MAX_IMAGE_BYTES = 5L * 1024L * 1024L;

    private ContentValidationUtil() {
    }

    public static String normalizeRequiredText(String value, String fieldName, int maxLength) {
        String normalized = normalizeOptionalText(value, fieldName, maxLength);
        if (normalized == null) {
            throw new BadRequestException(fieldName + " không được để trống");
        }
        return normalized;
    }

    public static String normalizeOptionalText(String value, String fieldName, int maxLength) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > maxLength) {
            throw new BadRequestException(fieldName + " không được vượt quá " + maxLength + " ký tự");
        }
        return normalized;
    }

    public static String normalizeOptionalUrl(String value, String fieldName, int maxLength) {
        String normalized = normalizeOptionalText(value, fieldName, maxLength);
        if (normalized == null) {
            return null;
        }

        try {
            URI uri = URI.create(normalized);
            String scheme = uri.getScheme();
            if (!StringUtils.hasText(scheme) || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                throw new IllegalArgumentException();
            }
            if (!StringUtils.hasText(uri.getHost())) {
                throw new IllegalArgumentException();
            }
            return normalized;
        } catch (Exception ex) {
            throw new BadRequestException(fieldName + " không đúng định dạng URL hợp lệ");
        }
    }

    public static String normalizeOptionalColorCode(String value) {
        String normalized = normalizeOptionalText(value, "Mã màu", 20);
        if (normalized == null) {
            return "#6366F1";
        }
        if (!normalized.matches("^#(?:[0-9A-Fa-f]{3}|[0-9A-Fa-f]{6})$")) {
            throw new BadRequestException("Mã màu phải ở dạng #RGB hoặc #RRGGBB");
        }
        return normalized;
    }

    public static String normalizeOptionalFeedbackCategory(String value) {
        String normalized = normalizeOptionalText(value, "Loại phản hồi", 50);
        if (normalized == null) {
            return null;
        }

        normalized = normalized.toUpperCase(Locale.ROOT);
        if (!ALLOWED_FEEDBACK_CATEGORIES.contains(normalized)) {
            throw new BadRequestException("Loại phản hồi không hợp lệ");
        }
        return normalized;
    }

    public static void validateImageFiles(List<MultipartFile> files, int maxCount) {
        if (files == null || files.isEmpty()) {
            return;
        }
        if (files.size() > maxCount) {
            throw new BadRequestException("Chỉ được tải lên tối đa " + maxCount + " ảnh");
        }

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            if (file.getSize() > DEFAULT_MAX_IMAGE_BYTES) {
                throw new BadRequestException("Mỗi ảnh tải lên không được vượt quá 5MB");
            }
            String contentType = file.getContentType();
            if (!StringUtils.hasText(contentType) || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
                throw new BadRequestException("Chỉ chấp nhận tệp hình ảnh hợp lệ");
            }
        }
    }
}
