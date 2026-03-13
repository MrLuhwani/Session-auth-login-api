package dev.luhwani.cookieLoginApi.dto;

import java.util.List;

public record UserRecord(
        String email,
        Long id,
        String username,
        String passwordHash,
        boolean enabled,
        List<String> authorities) {
}
