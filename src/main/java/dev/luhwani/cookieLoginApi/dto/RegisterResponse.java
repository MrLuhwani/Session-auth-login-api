package dev.luhwani.cookieLoginApi.dto;

import java.util.List;

public record RegisterResponse(
        String email,
        String username,
        List<String> authorities
) {}
