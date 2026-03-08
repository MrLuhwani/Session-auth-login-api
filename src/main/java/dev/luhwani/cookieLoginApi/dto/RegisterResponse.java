package dev.luhwani.cookieLoginApi.dto;

public record RegisterResponse(
        String message,
        Long userId,
        String username,
        String redirectTo
) {}

/*
package dev.luhwani.cookieLoginApi.dto;

import java.util.List;

public record RegisterResponse(
        String message,
        Long userId,
        String email,
        String username,
        List<String> authorities
) {} */