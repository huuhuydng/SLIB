package slib.com.example.exception;

/**
 * Exception thrown when client sends invalid request
 */
public class BadRequestException extends RuntimeException {
    
    public BadRequestException(String message) {
        super(message);
    }
}
