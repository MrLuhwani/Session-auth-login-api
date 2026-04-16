package dev.luhwani.cookieLoginApi.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.luhwani.cookieLoginApi.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JsonAuthenticationFailureHandler}.
 *
 * No Spring context. Tests the handler's logic, response format, Caffeine cache
 * interaction, and the static attempt-counter that drives account locking.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JsonAuthenticationFailureHandler")
class JsonAuthenticationFailureHandlerTest {

    @Mock
    private UserRepository userRepository;

    private Cache<String, String> lockedAccountsCache;
    private JsonAuthenticationFailureHandler handler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── setup 

    @BeforeEach
    void setUp() throws Exception {
        lockedAccountsCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(100)
                .build();

        handler = new JsonAuthenticationFailureHandler(objectMapper, userRepository, lockedAccountsCache);

        // Reset the static attempt counter so tests are isolated
        clearStaticAttemptMap();
    }

    /** Uses reflection to clear the private static ConcurrentHashMap. */
    private void clearStaticAttemptMap() throws Exception {
        Field field = JsonAuthenticationFailureHandler.class.getDeclaredField("emailToAttempts");
        field.setAccessible(true);
        ((Map<?, ?>) field.get(null)).clear();
    }

    // ── helper methods

    private MockHttpServletRequest req(String email, String ip) {
        MockHttpServletRequest r = new MockHttpServletRequest();
        if (email != null) r.setParameter("email", email);
        r.setRemoteAddr(ip != null ? ip : "127.0.0.1");
        return r;
    }

    private void fireFailures(String email, int count) throws Exception {
        for (int i = 0; i < count; i++) {
            handler.onAuthenticationFailure(
                    req(email, "127.0.0.1"),
                    new MockHttpServletResponse(),
                    new BadCredentialsException("bad")
            );
        }
    }

    // ── Response format

    @Nested
    @DisplayName("Response format")
    class ResponseFormat {

        @Test
        @DisplayName("status is 401")
        void status401() throws Exception {
            MockHttpServletResponse resp = new MockHttpServletResponse();
            handler.onAuthenticationFailure(req("u@t.com", "127.0.0.1"), resp, new BadCredentialsException("x"));
            assertThat(resp.getStatus()).isEqualTo(401);
        }

        @Test
        @DisplayName("Content-Type is application/json")
        void contentTypeJson() throws Exception {
            MockHttpServletResponse resp = new MockHttpServletResponse();
            handler.onAuthenticationFailure(req("u@t.com", "127.0.0.1"), resp, new BadCredentialsException("x"));
            assertThat(resp.getContentType()).isEqualTo("application/json");
        }

        @Test
        @DisplayName("error header is 'AuthenticationException'")
        void errorHeader() throws Exception {
            MockHttpServletResponse resp = new MockHttpServletResponse();
            handler.onAuthenticationFailure(req("u@t.com", "127.0.0.1"), resp, new BadCredentialsException("x"));
            assertThat(resp.getHeader("error")).isEqualTo("AuthenticationException");
        }

        @Test
        @DisplayName("body contains 'Invalid email or password' — no account enumeration")
        void genericMessage_noEnumeration() throws Exception {
            MockHttpServletResponse resp = new MockHttpServletResponse();
            handler.onAuthenticationFailure(req("u@t.com", "127.0.0.1"), resp, new BadCredentialsException("x"));
            JsonNode root = objectMapper.readTree(resp.getContentAsString());
            assertThat(root.path("success").asBoolean()).isFalse();
            assertThat(root.toString()).contains("Invalid email or password");
        }

        @Test
        @DisplayName("LockedException also returns the SAME generic message (prevents user enumeration)")
        void lockedException_sameGenericMessage() throws Exception {
            MockHttpServletResponse resp = new MockHttpServletResponse();
            handler.onAuthenticationFailure(req("locked@t.com", "127.0.0.1"), resp, new LockedException("locked"));
            assertThat(resp.getStatus()).isEqualTo(401);
            assertThat(resp.getContentAsString()).contains("Invalid email or password");
        }
    }

    // ── Null / blank email edge cases

    @Nested
    @DisplayName("Null / blank email edge cases")
    class NullBlankEmail {

        @Test
        @DisplayName("null email: returns 401, never touches the DB")
        void nullEmail_noDbCall() throws Exception {
            MockHttpServletResponse resp = new MockHttpServletResponse();
            handler.onAuthenticationFailure(req(null, "127.0.0.1"), resp, new BadCredentialsException("x"));
            assertThat(resp.getStatus()).isEqualTo(401);
            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("blank email: returns 401, never touches the DB")
        void blankEmail_noDbCall() throws Exception {
            MockHttpServletResponse resp = new MockHttpServletResponse();
            handler.onAuthenticationFailure(req("   ", "127.0.0.1"), resp, new BadCredentialsException("x"));
            assertThat(resp.getStatus()).isEqualTo(401);
            verifyNoInteractions(userRepository);
        }
    }

    // ── Attempt counting and locking logic

    @Nested
    @DisplayName("Attempt counting and account locking")
    class AttemptCounting {

        private static final String EMAIL = "victim@test.com";
        private static final String IP    = "10.0.0.1";

        @Test
        @DisplayName("6 failures (below MAX_ATTEMPTS=7) do NOT trigger a lock")
        void sixFailures_noLock() throws Exception {
            fireFailures(EMAIL, 6);
            verify(userRepository, never()).tempAcctLock(any(), any());
        }

        @Test
        @DisplayName("exactly 7 failures trigger tempAcctLock once")
        void sevenFailures_triggersLock() throws Exception {
            fireFailures(EMAIL, 7);
            verify(userRepository, times(1)).tempAcctLock(eq(EMAIL), any());
        }

        @Test
        @DisplayName("on lock, the email→IP mapping is put in the Caffeine cache")
        void onLock_cachedEmailToIp() throws Exception {
            for (int i = 0; i < 7; i++) {
                handler.onAuthenticationFailure(req(EMAIL, IP), new MockHttpServletResponse(),
                        new BadCredentialsException("bad"));
            }
            assertThat(lockedAccountsCache.getIfPresent(EMAIL)).isEqualTo(IP);
        }

        @Test
        @DisplayName("after lock, attempt counter is CLEARED — next failures require 7 more hits")
        void afterLock_counterReset_requiresSevenMoreToLockAgain() throws Exception {
            fireFailures(EMAIL, 7);       // triggers lock, clears counter
            reset(userRepository);        // clear mock state

            fireFailures(EMAIL, 6);       // 6 more — NOT enough
            verify(userRepository, never()).tempAcctLock(any(), any());

            handler.onAuthenticationFailure(req(EMAIL, IP), new MockHttpServletResponse(),
                    new BadCredentialsException("bad"));  // 7th — should lock again
            verify(userRepository, times(1)).tempAcctLock(eq(EMAIL), any());
        }

        @Test
        @DisplayName("different emails have independent counters")
        void differentEmails_independentCounters() throws Exception {
            fireFailures("alice@test.com", 6);
            fireFailures("bob@test.com", 6);
            verify(userRepository, never()).tempAcctLock(any(), any());

            // alice hits 7
            handler.onAuthenticationFailure(req("alice@test.com", "127.0.0.1"),
                    new MockHttpServletResponse(), new BadCredentialsException("x"));
            verify(userRepository, times(1)).tempAcctLock(eq("alice@test.com"), any());
            verify(userRepository, never()).tempAcctLock(eq("bob@test.com"), any());
        }
    }

    // ── LockedException handling

    @Nested
    @DisplayName("LockedException handling")
    class LockedExceptionHandling {

        @Test
        @DisplayName("LockedException adds email→IP to cache if not already present")
        void lockedEx_addsToCache() throws Exception {
            handler.onAuthenticationFailure(
                    req("locked@test.com", "192.168.1.1"),
                    new MockHttpServletResponse(),
                    new LockedException("locked")
            );
            assertThat(lockedAccountsCache.getIfPresent("locked@test.com")).isEqualTo("192.168.1.1");
        }

        @Test
        @DisplayName("LockedException does NOT overwrite an existing cache entry")
        void lockedEx_doesNotOverwriteExistingCacheEntry() throws Exception {
            lockedAccountsCache.put("locked@test.com", "original-ip");
            handler.onAuthenticationFailure(
                    req("locked@test.com", "attacker-ip"),
                    new MockHttpServletResponse(),
                    new LockedException("locked")
            );
            assertThat(lockedAccountsCache.getIfPresent("locked@test.com")).isEqualTo("original-ip");
        }

        @Test
        @DisplayName("LockedException never calls tempAcctLock (DB lock already set)")
        void lockedEx_neverCallsTempAcctLock() throws Exception {
            handler.onAuthenticationFailure(
                    req("locked@test.com", "127.0.0.1"),
                    new MockHttpServletResponse(),
                    new LockedException("locked")
            );
            verify(userRepository, never()).tempAcctLock(any(), any());
        }

        @Test
        @DisplayName("LockedException with null email: cache NOT polluted, no NPE")
        void lockedEx_nullEmail_noCacheEntry() throws Exception {
            handler.onAuthenticationFailure(
                    req(null, "127.0.0.1"),
                    new MockHttpServletResponse(),
                    new LockedException("locked")
            );
            // The cache should not have a null entry
            assertThat(lockedAccountsCache.asMap()).isEmpty();
        }
    }
}