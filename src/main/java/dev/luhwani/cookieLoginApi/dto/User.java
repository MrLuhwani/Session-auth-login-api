package dev.luhwani.cookieLoginApi.dto;

public record User(
        String email,
        Long id,
        String username,
        String passwordHash,
        boolean enabled,
        String authority) {
}

/*
package dev.luhwani.cookieLoginApi.dto;

public record UserRecord(
        Long id,
        String email,
        String username,
        String passwordHash,
        boolean enabled,
        String authority
) {}
 */