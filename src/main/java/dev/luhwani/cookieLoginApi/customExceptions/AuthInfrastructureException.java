package dev.luhwani.cookieLoginApi.customExceptions;

public class AuthInfrastructureException extends RuntimeException{
    
    public AuthInfrastructureException(String message, Throwable cause) {
        super(message, cause);
    }

    public AuthInfrastructureException(String message) {
        super(message);
    }

    public AuthInfrastructureException() {
    }
}
