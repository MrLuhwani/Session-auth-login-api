package dev.luhwani.cookieLoginApi.customExceptions;

public class DuplicateUsernameException extends RuntimeException {

    public DuplicateUsernameException(String message) {
        super(message);
    }
    
}
