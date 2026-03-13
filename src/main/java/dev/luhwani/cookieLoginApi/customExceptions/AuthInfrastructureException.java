package dev.luhwani.cookieLoginApi.customExceptions;

import org.springframework.dao.DataAccessException;

public class AuthInfrastructureException extends RuntimeException{

    public AuthInfrastructureException(String message, DataAccessException e) {
        super(message);
    }

    public AuthInfrastructureException(String message) {
        super(message);
    }
}
