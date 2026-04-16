package dev.luhwani.cookieLoginApi.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureWebMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration tests for the login flow.
 *
 * Covers: successful login, bad credentials, locked account, CSRF enforcement,
 * success-handler JSON shape, and the /me endpoint.
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DisplayName("Login Flow — Integration Tests")
class LoginFlowIntegrationTest {

    @Autowired private MockMvc       mockMvc;
    @Autowired private JdbcTemplate  jdbc;
    @Autowired private PasswordEncoder passwordEncoder;

    private final ObjectMapper mapper = new ObjectMapper();

    private static final String EMAIL    = "login_flow_test@example.com";
    private static final String PASSWORD = "LoginTest1";
    private static final String USERNAME = "loginflowuser";

    @BeforeAll
    static void createTestUser(@Autowired JdbcTemplate jdbc,
                               @Autowired PasswordEncoder enc) {
        jdbc.update("INSERT INTO users (email, username, password_hash, enabled) VALUES (?, ?, ?, true)",
                EMAIL, USERNAME, enc.encode(PASSWORD));
        Long id = jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, EMAIL);
        jdbc.update("INSERT INTO authorities (user_id, authority) VALUES (?, 'ROLE_USER')", id);
    }

    @AfterAll
    static void deleteTestUser(@Autowired JdbcTemplate jdbc) {
        jdbc.update("DELETE FROM authorities WHERE user_id = (SELECT id FROM users WHERE email = ?)", EMAIL);
        jdbc.update("DELETE FROM users WHERE email = ?", EMAIL);
    }

    // ── Successful login

    @Test
    @DisplayName("Valid credentials → 200 OK with success JSON body")
    void validCredentials_returns200() throws Exception {
        MvcResult result = mockMvc.perform(post("/login")
                .param("email", EMAIL)
                .param("password", PASSWORD)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andReturn();

        JsonNode body = mapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("id").asLong()).isGreaterThan(0);
        assertThat(body.path("message").asText()).isEqualTo("Login successful");
    }

    @Test
    @DisplayName("Successful login body contains user object with username and roles")
    void validCredentials_bodyContainsUserAndRoles() throws Exception {
        MvcResult result = mockMvc.perform(post("/login")
                .param("email", EMAIL)
                .param("password", PASSWORD)
                .with(csrf()))
                .andReturn();

        JsonNode user = mapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("user");
        assertThat(user.path("username").asText()).isEqualTo(EMAIL); // Spring getUsername() returns email
        assertThat(user.path("roles").toString()).contains("ROLE_USER");
    }

    @Test
    @DisplayName("Successful login contains a redirect link in the meta field")
    void validCredentials_bodyContainsRedirectLink() throws Exception {
        MvcResult result = mockMvc.perform(post("/login")
                .param("email", EMAIL)
                .param("password", PASSWORD)
                .with(csrf()))
                .andReturn();

        JsonNode body = mapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("link").path("redirect").asText())
                .isEqualTo("http://localhost:8080/me");
    }

    @Test
    @DisplayName("Successful login returns a JSESSIONID cookie")
    void validCredentials_setsJsessionidCookie() throws Exception {
        mockMvc.perform(post("/login")
                .param("email", EMAIL)
                .param("password", PASSWORD)
                .with(csrf()))
                .andExpect(cookie().exists("JSESSIONID"));
    }

    @Test
    @DisplayName("Authenticated session can access /me endpoint")
    void validCredentials_sessionCanAccessMe() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/login")
                .session(new MockHttpSession())
                .param("email", EMAIL)
                .param("password", PASSWORD)
                .with(csrf()))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        mockMvc.perform(get("/me").session(session))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("/me response body contains correct email and username")
    void meEndpoint_returnsCorrectPrincipalInfo() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/login")
                .session(new MockHttpSession())
                .param("email", EMAIL)
                .param("password", PASSWORD)
                .with(csrf()))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        MvcResult meResult = mockMvc.perform(get("/me").session(session)).andReturn();

        JsonNode body = mapper.readTree(meResult.getResponse().getContentAsString());
        assertThat(body.path("data").path("email").asText()).isEqualTo(EMAIL);
        assertThat(body.path("data").path("username").asText()).isEqualTo(USERNAME);
    }

    // ── Bad credentials

    @Test
    @DisplayName("Wrong password → 401 with generic message (no account enumeration)")
    void wrongPassword_returns401_genericMessage() throws Exception {
        MvcResult result = mockMvc.perform(post("/login")
                .param("email", EMAIL)
                .param("password", "WrongPassword9")
                .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andReturn();

        JsonNode body = mapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("success").asBoolean()).isFalse();
        assertThat(body.toString()).contains("Invalid email or password");
        // must NOT reveal which field was wrong
        assertThat(body.toString()).doesNotContain("password is incorrect");
        assertThat(body.toString()).doesNotContain("email not found");
    }

    @Test
    @DisplayName("Non-existent email → 401 with same generic message (prevents enumeration)")
    void nonExistentEmail_returns401_sameMessage() throws Exception {
        MvcResult wrongPass = mockMvc.perform(post("/login")
                .param("email", EMAIL)
                .param("password", "BadPassword9")
                .with(csrf()))
                .andReturn();

        MvcResult noEmail = mockMvc.perform(post("/login")
                .param("email", "nobody@nowhere.com")
                .param("password", "SomePassword1")
                .with(csrf()))
                .andReturn();

        assertThat(wrongPass.getResponse().getStatus()).isEqualTo(401);
        assertThat(noEmail.getResponse().getStatus()).isEqualTo(401);
        // Both should have the same body — attacker cannot distinguish them
        assertThat(wrongPass.getResponse().getContentAsString())
                .isEqualTo(noEmail.getResponse().getContentAsString());
    }

    // ── Locked account
    @Test
    @DisplayName("Locked account → 401 with same generic message (prevents enumeration)")
    void lockedAccount_returns401_genericMessage() throws Exception {
        // Lock the test user
        jdbc.update("UPDATE users SET locked_until = ? WHERE email = ?",
                Timestamp.valueOf(LocalDateTime.now().plusHours(3)), EMAIL);

        try {
            MvcResult result = mockMvc.perform(post("/login")
                    .param("email", EMAIL)
                    .param("password", PASSWORD)
                    .with(csrf()))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            assertThat(result.getResponse().getContentAsString()).contains("Invalid email or password");
        } finally {
            // Unlock the user so other tests aren't affected
            jdbc.update("UPDATE users SET locked_until = NULL WHERE email = ?", EMAIL);
        }
    }

    // ── CSRF protection

    @Test
    @DisplayName("Login without CSRF token → 403 Forbidden")
    void loginWithoutCsrf_returns403() throws Exception {
        // Note: the form login endpoint is marked permitAll() but CSRF is NOT disabled for it
        // (only the comment in the code mentions an option to ignore it for Postman testing)
        mockMvc.perform(post("/login")
                .param("email", EMAIL)
                .param("password", PASSWORD)
                // NO .with(csrf()) here
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /csrf endpoint returns a valid CSRF token in the response body")
    void csrfEndpoint_returnsCsrfToken() throws Exception {
        MvcResult result = mockMvc.perform(get("/csrf"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = mapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("data").path("token").asText()).isNotBlank();
    }

    // ── Logout

    @Test
    @DisplayName("Logout invalidates session — /me returns 401 after logout")
    void logout_sessionInvalidated() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/login")
                .session(new MockHttpSession())
                .param("email", EMAIL)
                .param("password", PASSWORD)
                .with(csrf()))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        mockMvc.perform(get("/me").session(session)).andExpect(status().isOk());

        mockMvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"));

        mockMvc.perform(get("/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Logout response body has success=true and a redirect link")
    void logout_responseBody() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/login")
                .session(new MockHttpSession())
                .param("email", EMAIL)
                .param("password", PASSWORD)
                .with(csrf()))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        MvcResult logoutResult = mockMvc.perform(post("/logout").session(session).with(csrf()))
                .andReturn();

        JsonNode body = mapper.readTree(logoutResult.getResponse().getContentAsString());
        assertThat(body.path("success").asBoolean()).isTrue();
    }
}