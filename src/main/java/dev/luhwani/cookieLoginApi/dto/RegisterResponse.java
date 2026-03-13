package dev.luhwani.cookieLoginApi.dto;

import java.util.List;

public record RegisterResponse(
        String message,
        String email,
        String username,
        List<String> authorities,
        String redirectTo
) {}
