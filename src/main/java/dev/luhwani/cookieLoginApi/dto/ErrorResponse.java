package dev.luhwani.cookieLoginApi.dto;

import java.util.Map;

public class ErrorResponse {
    private final Integer status;
    private Map<String, Object> errors;

    public ErrorResponse(int status, Map<String, Object> errors) {
        this.status = status;
        this.errors = errors;
    }

    public Integer getStatus() {
        return status;
    }

    public Map<String, Object> getErrors() {
        return errors;
    }

    public void setErrors(Map<String, Object> errors) {
        this.errors = errors;
    }
}