package dev.luhwani.finance_ledger.services;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import dev.luhwani.finance_ledger.customExceptions.AuthInfrastructureException;
import dev.luhwani.finance_ledger.customExceptions.InvalidEmailException;
import dev.luhwani.finance_ledger.customExceptions.InvalidLoginException;
import dev.luhwani.finance_ledger.customExceptions.UnknownDBException;
import dev.luhwani.finance_ledger.domain.errorMessage.ErrorMessage;

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
