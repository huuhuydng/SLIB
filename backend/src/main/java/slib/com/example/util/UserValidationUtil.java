package slib.com.example.util;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Pattern;

public final class UserValidationUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,63}$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern USER_CODE_PATTERN = Pattern.compile("^[A-Z0-9._-]{1,20}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{8,20}$");
    private static final int MAX_FULL_NAME_LENGTH = 255;

    private UserValidationUtil() {
    }

    public static String normalizeRequiredFullName(String value) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            throw new RuntimeException("Họ và tên không được để trống");
        }
        if (normalized.length() > MAX_FULL_NAME_LENGTH) {
            throw new RuntimeException("Họ và tên không được vượt quá 255 ký tự");
        }
        return normalized;
    }

    public static String normalizeRequiredEmail(String value) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            throw new RuntimeException("Email không được để trống");
        }

        normalized = normalized.toLowerCase(Locale.ROOT);
        if (normalized.length() > 255) {
            throw new RuntimeException("Email không được vượt quá 255 ký tự");
        }
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new RuntimeException("Email không đúng định dạng");
        }
        return normalized;
    }

    public static String normalizeRequiredUserCode(String value) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            throw new RuntimeException("Mã người dùng không được để trống");
        }

        normalized = normalized.toUpperCase(Locale.ROOT);
        if (!USER_CODE_PATTERN.matcher(normalized).matches()) {
            throw new RuntimeException(
                    "Mã người dùng chỉ được chứa chữ cái, số, dấu chấm, gạch ngang hoặc gạch dưới và tối đa 20 ký tự");
        }
        return normalized;
    }

    public static String normalizeOptionalPhone(String value) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            return null;
        }

        String compact = normalized.replaceAll("[\\s().-]", "");
        if (!PHONE_PATTERN.matcher(compact).matches()) {
            throw new RuntimeException("Số điện thoại phải có từ 8 đến 20 ký tự số và chỉ được chứa dấu + ở đầu");
        }
        return compact;
    }

    public static LocalDate validateOptionalDob(LocalDate dob) {
        if (dob == null) {
            return null;
        }
        if (dob.isAfter(LocalDate.now())) {
            throw new RuntimeException("Ngày sinh không được ở tương lai");
        }
        if (dob.isBefore(LocalDate.of(1900, 1, 1))) {
            throw new RuntimeException("Ngày sinh không hợp lệ");
        }
        return dob;
    }

    public static LocalDate parseOptionalDob(String value) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            return null;
        }

        try {
            return validateOptionalDob(LocalDate.parse(normalized));
        } catch (DateTimeParseException ex) {
            throw new RuntimeException("Ngày sinh phải đúng định dạng yyyy-MM-dd");
        }
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isEmpty() ? null : normalized;
    }
}
