package dev.luhwani.cookieLoginApi.dto;

public record RegisterRequest(
        String email,
        String username,
        String password) {
}

