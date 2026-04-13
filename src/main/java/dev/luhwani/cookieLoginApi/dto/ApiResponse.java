package dev.luhwani.cookieLoginApi.dto;

import java.util.Map;

public class ApiResponse<T> {
    private final Boolean success;
    private T data;
    private String message;
    private Map<String, Object> link;
    private ErrorResponse error;

    public ApiResponse(boolean success, T data) {
        this.success = success;
        this.data = data;
    }

    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public ApiResponse(boolean success, String message, Map<String, Object> link) {
        this.success = success;
        this.message = message;
        this.link = link;
    }


    public ApiResponse(boolean success, T data, String message, Map<String, Object> link) {
        this.success = success;
        this.data = data;
        this.message = message;
        this.link = link;
    }
    
    public ApiResponse(boolean success, ErrorResponse error) {
        this.success = success;
        this.error = error;
    }

    public boolean getSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setData(String message) {
        this.message = message;
    }

    public Map<String, Object> getLink() {
        return link;
    }

    public void setLink(Map<String, Object> link) {
        this.link = link;
    }

    public ErrorResponse getError() {
        return error;
    }
    
}
