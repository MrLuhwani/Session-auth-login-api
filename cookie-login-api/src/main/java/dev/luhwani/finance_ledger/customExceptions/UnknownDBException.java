package dev.luhwani.finance_ledger.customExceptions;

public class UnknownDBException extends RuntimeException {

    public UnknownDBException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
