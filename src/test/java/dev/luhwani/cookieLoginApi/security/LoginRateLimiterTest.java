package dev.luhwani.cookieLoginApi.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link LoginRateLimiter}.
 */
@DisplayName("LoginRateLimiter")
class LoginRateLimiterTest {

    private LoginRateLimiter rateLimiter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        rateLimiter = new LoginRateLimiter(objectMapper);
    }

    // ── helpers

    private MockHttpServletRequest loginRequest(String email, String ip) {
        MockHttpServletRequest r = new MockHttpServletRequest("POST", "/login");
        r.setRequestURI("/login");
        if (email != null) r.setParameter("email", email);
        r.setRemoteAddr(ip != null ? ip : "127.0.0.1");
        return r;
    }

    // ── Non-login path passthrough

    @Nested
    @DisplayName("Non-login paths pass through without rate limiting")
    class NonLoginPath {

        @Test
        @DisplayName("GET /me passes through immediately")
        void getMe_passesThroughFilter() throws Exception {
            MockHttpServletRequest r = new MockHttpServletRequest("GET", "/me");
            r.setRequestURI("/me");
            MockHttpServletResponse resp = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();
            rateLimiter.doFilter(r, resp, chain);
            assertThat(chain.getRequest()).isNotNull();    // chain was called
            assertThat(resp.getStatus()).isEqualTo(200);  // no override
        }

        @Test
        @DisplayName("POST /register passes through without consuming login tokens")
        void postRegister_passesThroughFilter() throws Exception {
            MockHttpServletRequest r = new MockHttpServletRequest("POST", "/register");
            r.setRequestURI("/register");
            MockFilterChain chain = new MockFilterChain();
            rateLimiter.doFilter(r, new MockHttpServletResponse(), chain);
            assertThat(chain.getRequest()).isNotNull();
        }
    }

    // ── Missing / blank email

    @Nested
    @DisplayName("Missing / blank email throws before rate-limit check")
    class MissingEmail {

        @Test
        @DisplayName("null email on /login throws AuthenticationCredentialsNotFoundException")
        void nullEmail_throwsException() {
            assertThatThrownBy(() ->
                rateLimiter.doFilter(loginRequest(null, "127.0.0.1"),
                        new MockHttpServletResponse(), new MockFilterChain())
            ).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        }

        @Test
        @DisplayName("whitespace-only email throws AuthenticationCredentialsNotFoundException")
        void blankEmail_throwsException() {
            assertThatThrownBy(() ->
                rateLimiter.doFilter(loginRequest("   ", "127.0.0.1"),
                        new MockHttpServletResponse(), new MockFilterChain())
            ).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        }
    }

    // ── Rate-limit header

    @Nested
    @DisplayName("Rate-limit response headers")
    class Headers {

        @Test
        @DisplayName("successful request has X-RateLimit-Remaining header")
        void successfulRequest_hasRemainingHeader() throws Exception {
            MockHttpServletResponse resp = new MockHttpServletResponse();
            rateLimiter.doFilter(loginRequest("u@u.com", "127.0.0.1"), resp, new MockFilterChain());
            assertThat(resp.getHeader("X-RateLimit-Remaining")).isNotNull();
        }

        @Test
        @DisplayName("X-RateLimit-Remaining decreases with each request")
        void remainingHeader_decreasesPerRequest() throws Exception {
            String ip = "5.5.5.5";
            MockHttpServletResponse r1 = new MockHttpServletResponse();
            MockHttpServletResponse r2 = new MockHttpServletResponse();
            rateLimiter.doFilter(loginRequest("a@a.com", ip), r1, new MockFilterChain());
            rateLimiter.doFilter(loginRequest("a@a.com", ip), r2, new MockFilterChain());
            long after1 = Long.parseLong(r1.getHeader("X-RateLimit-Remaining"));
            long after2 = Long.parseLong(r2.getHeader("X-RateLimit-Remaining"));
            assertThat(after2).isLessThan(after1);
        }
    }

    // ── Per-IP rate limiting

    @Nested
    @DisplayName("Per-IP rate limiting (capacity = 5)")
    class PerIpRateLimit {

        @Test
        @DisplayName("5 requests from same IP all succeed")
        void fiveRequests_allSucceed() throws Exception {
            String ip = "11.11.11.11";
            for (int i = 0; i < 5; i++) {
                MockHttpServletResponse resp = new MockHttpServletResponse();
                MockFilterChain chain = new MockFilterChain();
                rateLimiter.doFilter(loginRequest("u@u.com", ip), resp, chain);
                assertThat(resp.getStatus()).isNotEqualTo(429);
                assertThat(chain.getRequest()).isNotNull();
            }
        }

        @Test
        @DisplayName("6th request from same IP gets 429 Too Many Requests")
        void sixthRequest_gets429() throws Exception {
            String ip = "22.22.22.22";
            for (int i = 0; i < 5; i++) {
                rateLimiter.doFilter(loginRequest("u@u.com", ip),
                        new MockHttpServletResponse(), new MockFilterChain());
            }
            MockHttpServletResponse limited = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();
            rateLimiter.doFilter(loginRequest("u@u.com", ip), limited, chain);
            assertThat(limited.getStatus()).isEqualTo(429);
            assertThat(chain.getRequest()).isNull(); // chain NOT invoked
        }

        @Test
        @DisplayName("429 response contains correct headers and JSON body")
        void rateLimited_responseFormat() throws Exception {
            String ip = "33.33.33.33";
            for (int i = 0; i < 5; i++) {
                rateLimiter.doFilter(loginRequest("u@u.com", ip),
                        new MockHttpServletResponse(), new MockFilterChain());
            }
            MockHttpServletResponse resp = new MockHttpServletResponse();
            rateLimiter.doFilter(loginRequest("u@u.com", ip), resp, new MockFilterChain());

            assertThat(resp.getStatus()).isEqualTo(429);
            assertThat(resp.getContentType()).isEqualTo("application/json");
            assertThat(resp.getHeader("Retry-After")).isNotNull();
            assertThat(resp.getHeader("X-RateLimit-Remaining")).isEqualTo("0");

            JsonNode body = objectMapper.readTree(resp.getContentAsString());
            assertThat(body.path("success").asBoolean()).isFalse();
            assertThat(body.toString()).contains("Rate limit exceeded");
            assertThat(body.toString()).contains("retryAfterSeconds");
        }

        @Test
        @DisplayName("different IPs have completely independent buckets")
        void differentIps_independentBuckets() throws Exception {
            String ip1 = "44.44.44.44";
            String ip2 = "55.55.55.55";

            // drain ip1
            for (int i = 0; i < 5; i++) {
                rateLimiter.doFilter(loginRequest("u@u.com", ip1),
                        new MockHttpServletResponse(), new MockFilterChain());
            }

            // ip2 should be untouched
            MockHttpServletResponse resp = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();
            rateLimiter.doFilter(loginRequest("u@u.com", ip2), resp, chain);
            assertThat(resp.getStatus()).isNotEqualTo(429);
            assertThat(chain.getRequest()).isNotNull();
        }
    }
}