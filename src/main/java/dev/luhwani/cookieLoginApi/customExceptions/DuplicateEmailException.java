package dev.luhwani.cookieLoginApi.customExceptions;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String message, Throwable cause) {
        super(message, cause);
    }

    public DuplicateEmailException(String message) {
        super(message);
    }

    public DuplicateEmailException() {
    }
}
