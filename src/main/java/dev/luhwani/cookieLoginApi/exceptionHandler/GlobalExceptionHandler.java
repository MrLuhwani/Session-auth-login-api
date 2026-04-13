package dev.luhwani.cookieLoginApi.exceptionHandler;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import dev.luhwani.cookieLoginApi.customExceptions.AuthInfrastructureException;
import dev.luhwani.cookieLoginApi.customExceptions.DuplicateEmailException;
import dev.luhwani.cookieLoginApi.customExceptions.DuplicateUsernameException;
import dev.luhwani.cookieLoginApi.dto.ApiResponse;
import dev.luhwani.cookieLoginApi.dto.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleValidationError(MethodArgumentNotValidException ex) {
        Map<String, Object> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err -> {
            errors.put(err.getField(), err.getDefaultMessage());
        });
        var errorResponse = new ErrorResponse(400, errors);
        ApiResponse<ErrorResponse> response = new ApiResponse<>(false, errorResponse);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleDuplicateEmail(DuplicateEmailException ex) {
        var errorResponse = new ErrorResponse(409, Map.of("message", ex.getMessage()));
        ApiResponse<ErrorResponse> response = new ApiResponse<>(false, errorResponse);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(DuplicateUsernameException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleDuplicateUsername(DuplicateUsernameException ex) {
        var errorResponse = new ErrorResponse(409, Map.of("message", ex.getMessage()));
        ApiResponse<ErrorResponse> response = new ApiResponse<>(false, errorResponse);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(AuthInfrastructureException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleInfrastructure(AuthInfrastructureException ex) {
        var errorResponse = new ErrorResponse(500, Map.of("message", "Authentication infrastructure error"));
        ApiResponse<ErrorResponse> response = new ApiResponse<>(false, errorResponse);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleUserNotFound(UsernameNotFoundException e) {
        var errorResponse = new ErrorResponse(404, Map.of("message", "User not found"));
        ApiResponse<ErrorResponse> response = new ApiResponse<>(false, errorResponse);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class) 
    public ResponseEntity<ApiResponse<ErrorResponse>> handleAuthException(AuthenticationCredentialsNotFoundException ex){
        var errorResponse = new ErrorResponse(400, Map.of("message", ex.getMessage()));
        ApiResponse<ErrorResponse> response = new ApiResponse<>(false, errorResponse);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
