package dev.luhwani.cookieLoginApi.customExceptions;

public class UnknownDBException extends RuntimeException {

    public UnknownDBException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
