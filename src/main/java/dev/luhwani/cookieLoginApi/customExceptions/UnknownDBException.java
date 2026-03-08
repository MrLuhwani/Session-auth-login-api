package dev.luhwani.cookieLoginApi.customExceptions;

public class UnknownDBException extends RuntimeException {

    public UnknownDBException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public UnknownDBException(String message) {
        super(message);
    }

    public UnknownDBException() {
    }
}

//this might not be needed last last