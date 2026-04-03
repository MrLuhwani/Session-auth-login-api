package dev.luhwani.cookieLoginApi.dto;

import java.sql.Timestamp;
import java.util.List;

public record UserRecord(
        String email,
        Long id,
        String username,
        String passwordHash,
        boolean enabled,
        Timestamp lockedUntil,
        List<String> authorities
        ) {
}
