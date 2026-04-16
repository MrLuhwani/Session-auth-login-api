package dev.luhwani.cookieLoginApi.integration;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureWebMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests that verify the URL-based authorization rules
 * defined in {@code SecurityConfig.securityFilterChain()}.
 *
 *  / → public
 *  /login → public
 *  /register → public
 *  /csrf → public
 *  /me → ROLE_USER
 *  /admin/** → ROLE_ADMIN
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DisplayName("Endpoint Authorization — Integration Tests")
class SecurityAuthorizationTest {

    @Autowired private MockMvc        mockMvc;
    @Autowired private JdbcTemplate   jdbc;
    @Autowired private PasswordEncoder enc;

    private static final String USER_EMAIL    = "authz_user@example.com";
    private static final String ADMIN_EMAIL   = "authz_admin@example.com";
    private static final String PASSWORD      = "AuthzTest1";

    @BeforeAll
    static void createUsers(@Autowired JdbcTemplate jdbc, @Autowired PasswordEncoder enc) {
        String hash = enc.encode(PASSWORD);

        // Regular user
        jdbc.update("INSERT INTO users (email, username, password_hash, enabled) VALUES (?, ?, ?, true)",
                USER_EMAIL, "authzuser", hash);
        Long userId = jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, USER_EMAIL);
        jdbc.update("INSERT INTO authorities (user_id, authority) VALUES (?, 'ROLE_USER')", userId);

        // Admin
        jdbc.update("INSERT INTO users (email, username, password_hash, enabled) VALUES (?, ?, ?, true)",
                ADMIN_EMAIL, "authzadmin", hash);
        Long adminId = jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, ADMIN_EMAIL);
        jdbc.update("INSERT INTO authorities (user_id, authority) VALUES (?, 'ROLE_ADMIN')", adminId);
    }

    @AfterAll
    static void deleteUsers(@Autowired JdbcTemplate jdbc) {
        jdbc.update("DELETE FROM authorities WHERE user_id IN (SELECT id FROM users WHERE email IN (?, ?))",
                USER_EMAIL, ADMIN_EMAIL);
        jdbc.update("DELETE FROM users WHERE email IN (?, ?)", USER_EMAIL, ADMIN_EMAIL);
    }

    // ─ Helper method to perform login and return the session for authenticated requests ─

    private MockHttpSession loginAs(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/login")
                .session(new MockHttpSession())
                .param("email", email)
                .param("password", PASSWORD)
                .with(csrf()))
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    // ── Public endpoints
    @Test
    @DisplayName("GET /csrf is accessible without authentication")
    void csrf_publicAccess() throws Exception {
        mockMvc.perform(get("/csrf")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /login is accessible without authentication")
    void loginPage_publicAccess() throws Exception {
        // The login page itself (GET) should be accessible
        mockMvc.perform(get("/login")).andExpect(status().isOk());
    }

    // ── /me — requires ROLE_USER

    @Test
    @DisplayName("GET /me without auth → 401 Unauthorized")
    void me_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/me")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /me with ROLE_USER → 200 OK")
    void me_withUserRole_returns200() throws Exception {
        mockMvc.perform(get("/me").session(loginAs(USER_EMAIL)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /me with ROLE_ADMIN → 403 Forbidden (admin has ADMIN role only, not USER)")
    void me_withAdminRole_returns403() throws Exception {
        // The admin user only has ROLE_ADMIN, not ROLE_USER.
        // @PreAuthorize("hasRole('USER')") on /me will deny admin access.
        mockMvc.perform(get("/me").session(loginAs(ADMIN_EMAIL)))
                .andExpect(status().isForbidden());
    }

    // ── /admin/** — requires ROLE_ADMIN

    @Test
    @DisplayName("GET /admin/home without auth → 401 Unauthorized")
    void adminHome_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/admin/home")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /admin/home with ROLE_USER → 403 Forbidden")
    void adminHome_withUserRole_returns403() throws Exception {
        mockMvc.perform(get("/admin/home").session(loginAs(USER_EMAIL)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /admin/home with ROLE_ADMIN → 200 OK")
    void adminHome_withAdminRole_returns200() throws Exception {
        mockMvc.perform(get("/admin/home").session(loginAs(ADMIN_EMAIL)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Access denied response is JSON (not HTML) — JsonAccessDeniedHandler working")
    void accessDenied_responseIsJson() throws Exception {
        MvcResult result = mockMvc.perform(get("/admin/home").session(loginAs(USER_EMAIL)))
                .andReturn();
        assertThat(result.getResponse().getContentType())
                .contains("application/json");
        assertThat(result.getResponse().getContentAsString())
                .contains("Access denied");
    }

    @Test
    @DisplayName("Unauthenticated response is JSON (not HTML) — JsonAuthenticationEntryPoint working")
    void unauthenticated_responseIsJson() throws Exception {
        MvcResult result = mockMvc.perform(get("/me")).andReturn();
        assertThat(result.getResponse().getContentType()).contains("application/json");
        assertThat(result.getResponse().getContentAsString()).contains("Authentication required");
    }

    // ── Any other endpoint — requires authentication ──────────────────────────

    @Test
    @DisplayName("GET /some-random-protected-path without auth → 401 Unauthorized")
    void randomPath_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/some/random/path")).andExpect(status().isUnauthorized());
    }
}

/** Helper import for assertThat in non-test class */
class AssertHelper {
    static void assertThat(String value) {
        org.assertj.core.api.Assertions.assertThat(value);
    }
}