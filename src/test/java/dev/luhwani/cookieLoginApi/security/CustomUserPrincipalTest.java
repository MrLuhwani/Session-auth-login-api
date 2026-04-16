package dev.luhwani.cookieLoginApi.security;

import dev.luhwani.cookieLoginApi.dto.UserRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CustomUserPrincipal}.
 */
@DisplayName("CustomUserPrincipal")
class CustomUserPrincipalTest {

    private UserRecord userWith(boolean enabled, Timestamp lockedUntil) {
        return new UserRecord(
                "user@example.com",
                1L,
                "displayname",
                "$2a$10$somehash",
                enabled,
                lockedUntil,
                List.of("ROLE_USER"));
    }

    // ─ isAccountNonLocked

    @Nested
    @DisplayName("isAccountNonLocked()")
    class AccountLockStatus {

        @Test
        @DisplayName("returns TRUE when lockedUntil is null (never locked)")
        void nullLockedUntil_isNonLocked() {
            CustomUserPrincipal p = CustomUserPrincipal.fromUser(userWith(true, null));
            assertThat(p.isAccountNonLocked()).isTrue();
        }

        @Test
        @DisplayName("returns TRUE when lockedUntil is in the past (lock has expired)")
        void pastLockedUntil_isNonLocked() {
            Timestamp past = Timestamp.valueOf(LocalDateTime.now().minusHours(1));
            CustomUserPrincipal p = CustomUserPrincipal.fromUser(userWith(true, past));
            assertThat(p.isAccountNonLocked()).isTrue();
        }

        @Test
        @DisplayName("returns FALSE when lockedUntil is in the future (still locked)")
        void futureLockedUntil_isStillLocked() {
            Timestamp future = Timestamp.valueOf(LocalDateTime.now().plusHours(3));
            CustomUserPrincipal p = CustomUserPrincipal.fromUser(userWith(true, future));
            assertThat(p.isAccountNonLocked()).isFalse();
        }

        @Test
        @DisplayName("boundary: lockedUntil slightly in the future is considered locked")
        void boundaryFuture_isLocked() {
            Timestamp nearFuture = Timestamp.valueOf(LocalDateTime.now().plusSeconds(10));
            CustomUserPrincipal p = CustomUserPrincipal.fromUser(userWith(true, nearFuture));
            assertThat(p.isAccountNonLocked()).isFalse();
        }
    }

    // ── Username vs Email distinction ──

    @Nested
    @DisplayName("getUsername() / getEmail() / getDisplayUsername()")
    class IdentityFields {

        @Test
        @DisplayName("getUsername() returns the EMAIL (Spring Security login key)")
        void getUsername_returnsEmail() {
            CustomUserPrincipal p = CustomUserPrincipal.fromUser(userWith(true, null));
            assertThat(p.getUsername()).isEqualTo("user@example.com");
        }

        @Test
        @DisplayName("getEmail() also returns the email")
        void getEmail_returnsEmail() {
            CustomUserPrincipal p = CustomUserPrincipal.fromUser(userWith(true, null));
            assertThat(p.getEmail()).isEqualTo("user@example.com");
        }

        @Test
        @DisplayName("getDisplayUsername() returns the human-readable app username")
        void getDisplayUsername_returnsAppUsername() {
            CustomUserPrincipal p = CustomUserPrincipal.fromUser(userWith(true, null));
            assertThat(p.getDisplayUsername()).isEqualTo("displayname");
        }

        @Test
        @DisplayName("getUsername() and getDisplayUsername() are NOT the same")
        void usernameAndDisplayUsernameAreDifferent() {
            CustomUserPrincipal p = CustomUserPrincipal.fromUser(userWith(true, null));
            assertThat(p.getUsername()).isNotEqualTo(p.getDisplayUsername());
        }
    }

    // ── Authorities ──

    @Nested
    @DisplayName("getAuthorities()")
    class Authorities {

        @Test
        @DisplayName("single ROLE_USER authority is mapped correctly")
        void singleUserRole_mapped() {
            CustomUserPrincipal p = CustomUserPrincipal.fromUser(userWith(true, null));
            assertThat(p.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_USER");
        }

        @Test
        @DisplayName("multiple roles are all mapped")
        void multipleRoles_allMapped() {
            UserRecord user = new UserRecord(
                    "admin@example.com", 2L, "adminuser", "hash", true, null,
                    List.of("ROLE_USER", "ROLE_ADMIN"));
            CustomUserPrincipal p = CustomUserPrincipal.fromUser(user);
            assertThat(p.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactlyInAnyOrder(
                    "ROLE_USER",
                    "ROLE_ADMIN");
        }

        @Test
        @DisplayName("empty authorities list is handled gracefully")
        void emptyAuthorities_noException() {
            UserRecord user = new UserRecord("a@b.com", 3L, "user3", "hash", true, null, List.of());
            CustomUserPrincipal p = CustomUserPrincipal.fromUser(user);
            assertThat(p.getAuthorities()).isEmpty();
        }
    }

    // ── Other fields ──

    @Nested
    @DisplayName("Other UserDetails fields")
    class OtherFields {

        @Test
        @DisplayName("getPassword() returns the passwordHash field")
        void getPassword_returnsHash() {
            CustomUserPrincipal p = CustomUserPrincipal.fromUser(userWith(true, null));
            assertThat(p.getPassword()).isEqualTo("$2a$10$somehash");
        }

        @Test
        @DisplayName("isEnabled() reflects the enabled field — TRUE")
        void isEnabled_true() {
            assertThat(CustomUserPrincipal.fromUser(userWith(true, null)).isEnabled()).isTrue();
        }

        @Test
        @DisplayName("isEnabled() reflects the enabled field — FALSE")
        void isEnabled_false() {
            assertThat(CustomUserPrincipal.fromUser(userWith(false, null)).isEnabled()).isFalse();
        }

        @Test
        @DisplayName("getId() returns the correct id")
        void getId_correct() {
            CustomUserPrincipal p = CustomUserPrincipal.fromUser(userWith(true, null));
            assertThat(p.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("isAccountNonExpired() returns true (not implemented, defaults to true)")
        void isAccountNonExpired_alwaysTrue() {
            CustomUserPrincipal p = CustomUserPrincipal.fromUser(userWith(true, null));
            assertThat(p.isAccountNonExpired()).isTrue();
        }

        @Test
        @DisplayName("isCredentialsNonExpired() returns true (not implemented, defaults to true)")
        void isCredentialsNonExpired_alwaysTrue() {
            CustomUserPrincipal p = CustomUserPrincipal.fromUser(userWith(true, null));
            assertThat(p.isCredentialsNonExpired()).isTrue();
        }
    }
}