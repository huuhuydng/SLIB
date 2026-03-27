package slib.com.example.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Global Exception Handler for all REST Controllers
 * Provides centralized exception handling across all @RequestMapping methods
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

        /**
         * Handle AccessDeniedException (403)
         */
        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ErrorResponse> handleAccessDenied(
                        AccessDeniedException ex,
                        HttpServletRequest request) {
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.FORBIDDEN.value(),
                                "Forbidden",
                                "Bạn không có quyền truy cập tài nguyên này",
                                request.getRequestURI());
                return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
        }

        /**
         * Handle ResourceNotFoundException (404)
         */
        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleResourceNotFound(
                        ResourceNotFoundException ex,
                        HttpServletRequest request) {
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.NOT_FOUND.value(),
                                "Not Found",
                                ex.getMessage(),
                                request.getRequestURI());
                return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
        }

        /**
         * Handle NoSuchElementException (404)
         */
        @ExceptionHandler(NoSuchElementException.class)
        public ResponseEntity<ErrorResponse> handleNoSuchElement(
                        NoSuchElementException ex,
                        HttpServletRequest request) {
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.NOT_FOUND.value(),
                                "Not Found",
                                ex.getMessage() != null ? ex.getMessage() : "Không tìm thấy dữ liệu yêu cầu",
                                request.getRequestURI());
                return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
        }

        /**
         * Handle BadRequestException (400)
         */
        @ExceptionHandler(BadRequestException.class)
        public ResponseEntity<ErrorResponse> handleBadRequest(
                        BadRequestException ex,
                        HttpServletRequest request) {
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.BAD_REQUEST.value(),
                                "Bad Request",
                                ex.getMessage(),
                                request.getRequestURI());
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }

        /**
         * Handle IllegalArgumentException (400)
         */
        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ErrorResponse> handleIllegalArgument(
                        IllegalArgumentException ex,
                        HttpServletRequest request) {
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.BAD_REQUEST.value(),
                                "Bad Request",
                                ex.getMessage(),
                                request.getRequestURI());
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }

        /**
         * Handle jakarta validation constraint violations (400)
         */
        @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
        public ResponseEntity<Map<String, Object>> handleConstraintViolation(
                        jakarta.validation.ConstraintViolationException ex,
                        HttpServletRequest request) {
                Map<String, String> errors = ex.getConstraintViolations().stream()
                                .collect(Collectors.toMap(
                                                violation -> extractConstraintField(violation),
                                                ConstraintViolation::getMessage,
                                                (first, second) -> first,
                                                LinkedHashMap::new));

                return new ResponseEntity<>(
                                buildValidationResponse(
                                                HttpStatus.BAD_REQUEST,
                                                "Dữ liệu gửi lên không hợp lệ",
                                                request.getRequestURI(),
                                                errors),
                                HttpStatus.BAD_REQUEST);
        }

        /**
         * Handle validation errors from @Valid (400)
         */
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<Map<String, Object>> handleValidationErrors(
                        MethodArgumentNotValidException ex,
                        HttpServletRequest request) {
                Map<String, String> errors = new LinkedHashMap<>();

                ex.getBindingResult().getAllErrors().forEach(error -> {
                        String fieldName = ((FieldError) error).getField();
                        String errorMessage = error.getDefaultMessage();
                        errors.put(fieldName, errorMessage);
                });

                return new ResponseEntity<>(
                                buildValidationResponse(
                                                HttpStatus.BAD_REQUEST,
                                                "Dữ liệu gửi lên không hợp lệ",
                                                request.getRequestURI(),
                                                errors),
                                HttpStatus.BAD_REQUEST);
        }

        /**
         * Handle invalid JSON or malformed request body (400)
         */
        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
                        HttpMessageNotReadableException ex,
                        HttpServletRequest request) {
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.BAD_REQUEST.value(),
                                "Bad Request",
                                resolveReadableRequestBodyMessage(ex),
                                request.getRequestURI());
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }

        /**
         * Handle missing request parameters (400)
         */
        @ExceptionHandler(MissingServletRequestParameterException.class)
        public ResponseEntity<ErrorResponse> handleMissingParams(
                        MissingServletRequestParameterException ex,
                        HttpServletRequest request) {
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.BAD_REQUEST.value(),
                                "Bad Request",
                                String.format("Thiếu tham số bắt buộc '%s'", ex.getParameterName()),
                                request.getRequestURI());
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }

        /**
         * Handle missing multipart file parameter (400)
         */
        @ExceptionHandler(org.springframework.web.multipart.support.MissingServletRequestPartException.class)
        public ResponseEntity<ErrorResponse> handleMissingServletRequestPart(
                        org.springframework.web.multipart.support.MissingServletRequestPartException ex,
                        HttpServletRequest request) {
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.BAD_REQUEST.value(),
                                "Bad Request",
                                String.format("Thiếu phần dữ liệu bắt buộc '%s'", ex.getRequestPartName()),
                                request.getRequestURI());
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }

        /**
         * Handle type mismatch (e.g., string instead of number) (400)
         */
        @ExceptionHandler(MethodArgumentTypeMismatchException.class)
        public ResponseEntity<ErrorResponse> handleTypeMismatch(
                        MethodArgumentTypeMismatchException ex,
                        HttpServletRequest request) {
                String message = String.format("Tham số '%s' phải có kiểu dữ liệu %s",
                                ex.getName(),
                                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.BAD_REQUEST.value(),
                                "Bad Request",
                                message,
                                request.getRequestURI());
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }

        /**
         * Handle 404 Not Found for resources (404)
         */
        @ExceptionHandler(NoResourceFoundException.class)
        public ResponseEntity<ErrorResponse> handleNoResourceFound(
                        NoResourceFoundException ex,
                        HttpServletRequest request) {
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.NOT_FOUND.value(),
                                "Not Found",
                                "Endpoint yêu cầu không tồn tại",
                                request.getRequestURI());
                return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
        }

        /**
         * Handle DataIntegrityViolationException (409)
         */
        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
                        DataIntegrityViolationException ex,
                        HttpServletRequest request) {
                log.warn("DataIntegrityViolation at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.CONFLICT.value(),
                                "Conflict",
                                resolveDataIntegrityMessage(ex),
                                request.getRequestURI());
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }

        /**
         * Handle all other RuntimeExceptions (500)
         */
        @ExceptionHandler(RuntimeException.class)
        public ResponseEntity<ErrorResponse> handleRuntimeException(
                        RuntimeException ex,
                        HttpServletRequest request) {
                log.error("RuntimeException at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

                ErrorResponse error = new ErrorResponse(
                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                "Internal Server Error",
                                resolveRuntimeMessage(ex),
                                request.getRequestURI());
                return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        /**
         * Handle all other uncaught exceptions (500)
         */
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGlobalException(
                        Exception ex,
                        HttpServletRequest request) {
                log.error("Unhandled exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

                ErrorResponse error = new ErrorResponse(
                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                "Internal Server Error",
                                resolveUnexpectedMessage(ex),
                                request.getRequestURI());
                return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        private Map<String, Object> buildValidationResponse(
                        HttpStatus status,
                        String message,
                        String path,
                        Map<String, String> errors) {
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("timestamp", LocalDateTime.now());
                response.put("status", status.value());
                response.put("error", status.getReasonPhrase());
                response.put("message", message);
                response.put("errors", errors);
                response.put("path", path);
                return response;
        }

        private String extractConstraintField(ConstraintViolation<?> violation) {
                String path = violation.getPropertyPath() != null ? violation.getPropertyPath().toString() : null;
                if (!StringUtils.hasText(path)) {
                        return "request";
                }

                int dotIndex = path.lastIndexOf('.');
                return dotIndex >= 0 ? path.substring(dotIndex + 1) : path;
        }

        private String resolveReadableRequestBodyMessage(HttpMessageNotReadableException ex) {
                String mostSpecificMessage = extractMostSpecificMessage(ex);
                if (!StringUtils.hasText(mostSpecificMessage)) {
                        return "Body request không đúng định dạng JSON hoặc chứa giá trị không hợp lệ";
                }

                if (containsAny(mostSpecificMessage, "java.time.localdate", "localdate")) {
                        return "Ngày tháng không đúng định dạng. Hãy dùng định dạng yyyy-MM-dd";
                }

                if (containsAny(mostSpecificMessage, "cannot deserialize value of type", "from string")) {
                        return "Dữ liệu gửi lên có kiểu không đúng với định nghĩa của API";
                }

                if (containsAny(mostSpecificMessage, "unexpected character", "json parse error")) {
                        return "Body request không đúng định dạng JSON";
                }

                return "Body request không hợp lệ: " + sanitizeMessage(mostSpecificMessage);
        }

        private String resolveDataIntegrityMessage(DataIntegrityViolationException ex) {
                String message = extractMostSpecificMessage(ex);
                if (!StringUtils.hasText(message)) {
                        return "Không thể lưu dữ liệu vì vi phạm ràng buộc trong cơ sở dữ liệu";
                }

                String normalized = message.toLowerCase();

                if (normalized.contains("duplicate key") || normalized.contains("unique constraint")
                                || normalized.contains("unique index")) {
                        return "Dữ liệu bị trùng với bản ghi đã tồn tại";
                }

                if (normalized.contains("foreign key")) {
                        return "Dữ liệu tham chiếu không hợp lệ hoặc bản ghi liên quan không tồn tại";
                }

                if (normalized.contains("not-null") || normalized.contains("null value")) {
                        return "Thiếu dữ liệu bắt buộc, không thể lưu vào hệ thống";
                }

                if (normalized.contains("value too long") || normalized.contains("too long for type")
                                || normalized.contains("character varying")) {
                        return "Một số trường vượt quá độ dài cho phép của cơ sở dữ liệu";
                }

                if (normalized.contains("check constraint")) {
                        return "Dữ liệu không thỏa điều kiện hợp lệ của hệ thống";
                }

                return "Lỗi ràng buộc dữ liệu: " + sanitizeMessage(message);
        }

        private String resolveRuntimeMessage(RuntimeException ex) {
                if (StringUtils.hasText(ex.getMessage())) {
                        return sanitizeMessage(ex.getMessage());
                }

                String causeMessage = extractMostSpecificMessage(ex);
                if (StringUtils.hasText(causeMessage)) {
                        return "Đã xảy ra lỗi khi xử lý yêu cầu: " + sanitizeMessage(causeMessage);
                }

                return "Đã xảy ra lỗi trong quá trình xử lý yêu cầu";
        }

        private String resolveUnexpectedMessage(Exception ex) {
                String causeMessage = extractMostSpecificMessage(ex);
                if (StringUtils.hasText(causeMessage)) {
                        return "Đã xảy ra lỗi không mong muốn: " + sanitizeMessage(causeMessage);
                }
                return "Đã xảy ra lỗi không mong muốn. Vui lòng thử lại sau";
        }

        private String extractMostSpecificMessage(Throwable throwable) {
                if (throwable == null) {
                        return null;
                }

                Throwable current = throwable;
                while (current.getCause() != null && current.getCause() != current) {
                        current = current.getCause();
                }

                if (current instanceof SQLException sqlException && StringUtils.hasText(sqlException.getMessage())) {
                        return sqlException.getMessage();
                }

                if (StringUtils.hasText(current.getMessage())) {
                        return current.getMessage();
                }

                return throwable.getMessage();
        }

        private boolean containsAny(String value, String... keywords) {
                String normalized = value.toLowerCase();
                for (String keyword : keywords) {
                        if (normalized.contains(keyword.toLowerCase())) {
                                return true;
                        }
                }
                return false;
        }

        private String sanitizeMessage(String message) {
                if (!StringUtils.hasText(message)) {
                        return message;
                }

                String sanitized = message.replaceAll("\\s+", " ").trim();
                if (sanitized.length() > 300) {
                        return sanitized.substring(0, 300) + "...";
                }
                return sanitized;
        }
}
