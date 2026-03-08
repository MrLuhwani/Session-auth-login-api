package dev.luhwani.cookieLoginApi.customExceptions;

public class DuplicateUsernameException extends RuntimeException {
    
    public DuplicateUsernameException(String message, Throwable cause) {
        super(message, cause);
    }

    public DuplicateUsernameException(String message) {
        super(message);
    }

    public DuplicateUsernameException() {
    }

}
