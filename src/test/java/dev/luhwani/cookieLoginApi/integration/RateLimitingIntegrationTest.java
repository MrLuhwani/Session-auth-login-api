package dev.luhwani.cookieLoginApi.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureWebMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests verifying that rate-limiting is applied to:
 *
 *  /login — with {@link dev.luhwani.cookieLoginApi.security.LoginRateLimiter} (Spring Filter)
 *  /register — with {@link dev.luhwani.cookieLoginApi.rateLimiter.RateLimiterInterceptor} (MVC Interceptor)
 *
 * The per-IP limits are:
 *   /login          → 5 req/min per IP
 *   /register       → 7 req/2min per IP
 *   /admin/register → 3 req/2min per IP
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DisplayName("Rate Limiting — Integration Tests")
class RateLimitingIntegrationTest {

    @Autowired private MockMvc      mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PasswordEncoder enc;

    private final ObjectMapper mapper = new ObjectMapper();

    // Counter for generating unique IPs and usernames per test
    private static int testCounter = 0;

    @BeforeAll
    static void resetLoginBuckets() throws Exception {
        // Clear the LoginRateLimiter's per-IP bucket map so tests start fresh
        Field field = dev.luhwani.cookieLoginApi.security.LoginRateLimiter.class
                .getDeclaredField("buckets");
        field.setAccessible(true);
        ((Map<?, ?>) field.get(null)).clear();
    }

    @BeforeEach
    void incrementCounter() {
        testCounter++;
    }

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM authorities WHERE user_id IN " +
                "(SELECT id FROM users WHERE email LIKE '%@ratetest.com')");
        jdbc.update("DELETE FROM users WHERE email LIKE '%@ratetest.com'");
    }

    /** Returns a unique fake IP per test invocation. */
    private String uniqueIp() {
        return String.format("10.%d.%d.%d", testCounter / 65536, (testCounter / 256) % 256, testCounter % 256);
    }

    private String registerBody(String emailPrefix, String usernamePrefix) {
        return String.format(
                "{\"email\":\"%s%d@ratetest.com\",\"username\":\"%s%d\",\"password\":\"Password1\"}",
                emailPrefix, testCounter, usernamePrefix, testCounter
        );
    }

    // ── /login rate limiting

    @Nested
    @DisplayName("/login rate limiting (LoginRateLimiter filter)")
    class LoginRateLimiting {

        @Test
        @DisplayName("First 5 login attempts from one IP are NOT rate-limited")
        void loginFiveAttempts_notLimited() throws Exception {
            String ip = uniqueIp();
            for (int i = 0; i < 5; i++) {
                MvcResult r = mockMvc.perform(post("/login")
                        .with(request -> { request.setRemoteAddr(ip); return request; })
                        .param("email", "nobody@nobody.com")
                        .param("password", "BadPassword1")
                        .with(csrf()))
                        .andReturn();
                // Should be 401 (bad creds) or 200, NOT 429
                assertThat(r.getResponse().getStatus())
                        .as("Request %d should not be rate-limited", i + 1)
                        .isNotEqualTo(429);
            }
        }

        @Test
        @DisplayName("6th login attempt from same IP gets 429 Too Many Requests")
        void loginSixthAttempt_gets429() throws Exception {
            String ip = uniqueIp();
            for (int i = 0; i < 5; i++) {
                mockMvc.perform(post("/login")
                        .with(req -> { req.setRemoteAddr(ip); return req; })
                        .param("email", "nobody@nobody.com")
                        .param("password", "BadPassword1")
                        .with(csrf()))
                        .andReturn();
            }
            MvcResult r = mockMvc.perform(post("/login")
                    .with(req -> { req.setRemoteAddr(ip); return req; })
                    .param("email", "nobody@nobody.com")
                    .param("password", "BadPassword1")
                    .with(csrf()))
                    .andReturn();

            assertThat(r.getResponse().getStatus()).isEqualTo(429);
        }

        @Test
        @DisplayName("429 response sets correct headers (Retry-After, X-RateLimit-Remaining)")
        void loginRateLimited_responseHeaders() throws Exception {
            String ip = uniqueIp();
            // Drain the bucket
            for (int i = 0; i <= 5; i++) {
                mockMvc.perform(post("/login")
                        .with(req -> { req.setRemoteAddr(ip); return req; })
                        .param("email", "nobody@nobody.com")
                        .param("password", "BadPass1")
                        .with(csrf()));
            }
            MvcResult last = mockMvc.perform(post("/login")
                    .with(req -> { req.setRemoteAddr(ip); return req; })
                    .param("email", "nobody@nobody.com")
                    .param("password", "BadPass1")
                    .with(csrf()))
                    .andReturn();

            assertThat(last.getResponse().getStatus()).isEqualTo(429);
            assertThat(last.getResponse().getHeader("Retry-After")).isNotNull();
            assertThat(last.getResponse().getHeader("X-RateLimit-Remaining")).isEqualTo("0");
        }

        @Test
        @DisplayName("429 login response has JSON body with success=false and retryAfterSeconds")
        void loginRateLimited_responseBody() throws Exception {
            String ip = uniqueIp();
            MvcResult limited = null;
            for (int i = 0; i <= 5; i++) {
                limited = mockMvc.perform(post("/login")
                        .with(req -> { req.setRemoteAddr(ip); return req; })
                        .param("email", "nobody@nobody.com")
                        .param("password", "BadPass1")
                        .with(csrf()))
                        .andReturn();
            }
            assert limited != null;
            JsonNode body = mapper.readTree(limited.getResponse().getContentAsString());
            assertThat(body.path("success").asBoolean()).isFalse();
            assertThat(body.path("error").path("errors").path("retryAfterSeconds").asLong())
                    .isGreaterThan(0);
        }
        @Test
        @DisplayName("X-RateLimit-Remaining header is present on successful login requests")
        void login_successfulRequest_rateLimitHeader() throws Exception {
            String ip = uniqueIp();
            MvcResult r = mockMvc.perform(post("/login")
                    .with(req -> { req.setRemoteAddr(ip); return req; })
                    .param("email", "nobody@nobody.com")
                    .param("password", "BadPass1")
                    .with(csrf()))
                    .andReturn();

            // Not a 429, and should have the header
            assertThat(r.getResponse().getStatus()).isNotEqualTo(429);
            assertThat(r.getResponse().getHeader("X-RateLimit-Remaining")).isNotNull();
        }

        @Test
        @DisplayName("Login request with no email parameter throws before rate-limit check")
        void loginNoEmail_throwsBeforeRateLimit() throws Exception {
            // This should result in a 400 from the GlobalExceptionHandler,
            // NOT a 429 rate limit error — the email check happens first in the filter
            String ip = uniqueIp();
            MvcResult r = mockMvc.perform(post("/login")
                    .with(req -> { req.setRemoteAddr(ip); return req; })
                    // No 'email' param
                    .param("password", "BadPass1")
                    .with(csrf()))
                    .andReturn();

            // Should be 400 (handled by GlobalExceptionHandler) not 429
            assertThat(r.getResponse().getStatus()).isEqualTo(400);
        }
    }

    // ── /register rate limiting

    @Nested
    @DisplayName("/register rate limiting (RateLimiterInterceptor)")
    class RegisterRateLimiting {

        @Test
        @DisplayName("First 7 register attempts from one IP succeed (per-IP limit = 7)")
        void registerSevenAttempts_notLimited() throws Exception {
            String ip = uniqueIp();
            for (int i = 0; i < 7; i++) {
                final int idx = i;
                MvcResult r = mockMvc.perform(post("/register")
                        .with(req -> { req.setRemoteAddr(ip); return req; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                "{\"email\":\"rlimit%d_%d@ratetest.com\",\"username\":\"rlimituser%d%d\",\"password\":\"Password1\"}",
                                testCounter, idx, testCounter, idx
                        ))
                        .with(csrf()))
                        .andReturn();
                assertThat(r.getResponse().getStatus())
                        .as("Request %d should not be rate-limited", idx + 1)
                        .isNotEqualTo(429);
            }
        }

        @Test
        @DisplayName("8th register from same IP gets 429")
        void registerEighthAttempt_gets429() throws Exception {
            String ip = uniqueIp();
            for (int i = 0; i < 7; i++) {
                final int idx = i;
                mockMvc.perform(post("/register")
                        .with(req -> { req.setRemoteAddr(ip); return req; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                "{\"email\":\"r8th%d_%d@ratetest.com\",\"username\":\"r8thuser%d%d\",\"password\":\"Password1\"}",
                                testCounter, idx, testCounter, idx
                        ))
                        .with(csrf()));
            }
            MvcResult r = mockMvc.perform(post("/register")
                    .with(req -> { req.setRemoteAddr(ip); return req; })
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(String.format(
                            "{\"email\":\"r8thfinal%d@ratetest.com\",\"username\":\"r8thfinal%d\",\"password\":\"Password1\"}",
                            testCounter, testCounter
                    ))
                    .with(csrf()))
                    .andReturn();

            assertThat(r.getResponse().getStatus()).isEqualTo(429);
        }
    }
}