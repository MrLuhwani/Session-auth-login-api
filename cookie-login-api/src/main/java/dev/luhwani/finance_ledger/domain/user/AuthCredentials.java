package dev.luhwani.finance_ledger.domain.user;

public record AuthCredentials(
        Long id,
        String email,
        String username,
        byte[] passwordHash,
        String salt
) {
}