package dev.luhwani.cookieLoginApi.services;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import dev.luhwani.cookieLoginApi.customExceptions.AuthInfrastructureException;
import dev.luhwani.cookieLoginApi.customExceptions.BadRequestException;
import dev.luhwani.cookieLoginApi.customExceptions.DuplicateEmailException;
import dev.luhwani.cookieLoginApi.customExceptions.DuplicateUsernameException;
import dev.luhwani.cookieLoginApi.customExceptions.UnknownDBException;

enum ErrorMessage {
    BAD_REQUEST,
    AUTH_INFRASTRUCTURE_ERROR,
    UNKNOWN_DB_ERROR,
    DUPLICATE_EMAIL,
    DUPLICATE_USERNAME,
    INVALID_CREDENTIALS
}


@RestControllerAdvice
public class ExceptionHandlerService {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorMessage> badRequest() {  
        var errorMsg = ErrorMessage.BAD_REQUEST;
        return ResponseEntity.badRequest().body(errorMsg);
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorMessage> duplicateEmail() {  
        var errorMsg = ErrorMessage.DUPLICATE_EMAIL;
        return ResponseEntity.internalServerError().body(errorMsg);
    }

    @ExceptionHandler(DuplicateUsernameException.class)
    public ResponseEntity<ErrorMessage> duplicateUsername() {  
        var errorMsg = ErrorMessage.DUPLICATE_USERNAME;
        return ResponseEntity.internalServerError().body(errorMsg);
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

/*
package dev.luhwani.cookieLoginApi.web;

import dev.luhwani.cookieLoginApi.customExceptions.AuthInfrastructureException;
import dev.luhwani.cookieLoginApi.customExceptions.BadRequestException;
import dev.luhwani.cookieLoginApi.customExceptions.DuplicateEmailException;
import dev.luhwani.cookieLoginApi.customExceptions.DuplicateUsernameException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(BadRequestException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateEmail(DuplicateEmailException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(DuplicateUsernameException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateUsername(DuplicateUsernameException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(AuthInfrastructureException.class)
    public ResponseEntity<Map<String, Object>> handleInfrastructure(AuthInfrastructureException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "message", "Authentication infrastructure error"
        ));
    }
}
 */