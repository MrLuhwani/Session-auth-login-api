package dev.luhwani.cookieLoginApi.services;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import dev.luhwani.cookieLoginApi.customExceptions.AuthInfrastructureException;
import dev.luhwani.cookieLoginApi.customExceptions.InvalidEmailException;
import dev.luhwani.cookieLoginApi.customExceptions.InvalidLoginException;
import dev.luhwani.cookieLoginApi.customExceptions.UnknownDBException;
import dev.luhwani.cookieLoginApi.domain.errorMessage.ErrorMessage;

@RestControllerAdvice
public class ExceptionHandlerService {
    
    @ExceptionHandler(InvalidEmailException.class)
    public ResponseEntity<ErrorMessage> invalidEmailFormat() {
        var errorMsg = ErrorMessage.INVALID_EMAIL;
        return ResponseEntity.badRequest().body(errorMsg);
    }

    @ExceptionHandler(InvalidLoginException.class)
    public ResponseEntity<ErrorMessage> invalidLoginCredentials() {
        var errorMsg = ErrorMessage.INVALID_LOGIN_CREDENTIALS;
        return ResponseEntity.badRequest().body(errorMsg);
    }

    @ExceptionHandler(AuthInfrastructureException.class)
    public ResponseEntity<ErrorMessage> authInfrastructureError() {
        var errorMsg = ErrorMessage.AUTH_INFRASTRUCTURE_ERROR;
        return ResponseEntity.internalServerError().body(errorMsg);
    }

    @ExceptionHandler(UnknownDBException.class)
    public ResponseEntity<ErrorMessage> unknownDBError() {  
        var errorMsg = ErrorMessage.UNKNOWN_DB_ERROR;
        return ResponseEntity.internalServerError().body(errorMsg);
    }
}
