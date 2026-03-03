package dev.luhwani.finance_ledger.customExceptions;

public class InvalidLoginException extends RuntimeException {
    
    public InvalidLoginException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidLoginException(String message) {
        super(message);
    }

    public InvalidLoginException() {
    }
    
}
