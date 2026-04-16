package dev.luhwani.cookieLoginApi.rateLimiter;

import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link RateLimiterService}.
 *
 * Verifies bucket creation, capacity settings, per-IP isolation, and
 * key resolution — no Spring context required.
 */
@DisplayName("RateLimiterService")
class RateLimiterServiceTest {

    private RateLimiterService service;

    @BeforeEach
    void setUp() {
        service = new RateLimiterService();
    }

    // ── Bucket creation ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Bucket creation")
    class BucketCreation {

        @Test
        @DisplayName("'register' bucket has capacity 7")
        void registerBucket_capacitySeven() {
            Bucket b = service.resolveBucket("1.1.1.1", "register");
            assertThat(b.getAvailableTokens()).isEqualTo(7);
        }

        @Test
        @DisplayName("'admin-register' bucket has capacity 3")
        void adminRegisterBucket_capacityThree() {
            Bucket b = service.resolveBucket("1.1.1.1", "admin-register");
            assertThat(b.getAvailableTokens()).isEqualTo(3);
        }

        @Test
        @DisplayName("unknown endpoint key throws IllegalArgumentException")
        void unknownKey_throwsIllegalArgument() {
            assertThatThrownBy(() -> service.resolveBucket("1.1.1.1", "some-unknown-path"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown endpoint key");
        }
    }

    // ── Bucket identity / isolation ──────────────────────────────────────────

    @Nested
    @DisplayName("Bucket identity and per-IP isolation")
    class BucketIsolation {

        @Test
        @DisplayName("same IP + same endpoint always returns the SAME bucket instance (compute-if-absent)")
        void sameIpSameEndpoint_sameInstance() {
            Bucket first  = service.resolveBucket("10.0.0.1", "register");
            Bucket second = service.resolveBucket("10.0.0.1", "register");
            assertThat(first).isSameAs(second);
        }

        @Test
        @DisplayName("different IPs get DIFFERENT bucket instances")
        void differentIps_differentInstances() {
            Bucket a = service.resolveBucket("10.0.0.1", "register");
            Bucket b = service.resolveBucket("10.0.0.2", "register");
            assertThat(a).isNotSameAs(b);
        }

        @Test
        @DisplayName("same IP but different endpoints get DIFFERENT buckets")
        void sameIpDifferentEndpoints_differentInstances() {
            Bucket reg   = service.resolveBucket("10.0.0.1", "register");
            Bucket admin = service.resolveBucket("10.0.0.1", "admin-register");
            assertThat(reg).isNotSameAs(admin);
        }

        @Test
        @DisplayName("exhausting one IP's bucket does NOT affect a different IP's bucket")
        void exhaustingOneIp_doesNotAffectAnotherIp() {
            Bucket victim = service.resolveBucket("10.0.0.5", "register");
            // drain it
            for (int i = 0; i < 7; i++) victim.tryConsume(1);
            assertThat(victim.getAvailableTokens()).isZero();

            Bucket innocent = service.resolveBucket("10.0.0.6", "register");
            assertThat(innocent.getAvailableTokens()).isEqualTo(7); // untouched
        }
    }

    // ── Token consumption ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Token consumption")
    class TokenConsumption {

        @Test
        @DisplayName("consuming a token decreases available tokens by 1")
        void consume_decreasesTokens() {
            Bucket b = service.resolveBucket("192.168.0.1", "register");
            long before = b.getAvailableTokens();
            b.tryConsume(1);
            assertThat(b.getAvailableTokens()).isEqualTo(before - 1);
        }

        @Test
        @DisplayName("consuming all tokens: tryConsume returns false when empty")
        void consumeAll_returnsFalseWhenEmpty() {
            Bucket b = service.resolveBucket("192.168.0.2", "admin-register");
            for (int i = 0; i < 3; i++) b.tryConsume(1);
            assertThat(b.tryConsume(1)).isFalse();
        }

        @Test
        @DisplayName("admin-register bucket tracks independently — 3 consumes empties it")
        void adminBucketExhaustedAfterThreeConsumes() {
            Bucket b = service.resolveBucket("192.168.0.3", "admin-register");
            b.tryConsume(1);
            b.tryConsume(1);
            b.tryConsume(1);
            assertThat(b.getAvailableTokens()).isZero();
        }
    }

}