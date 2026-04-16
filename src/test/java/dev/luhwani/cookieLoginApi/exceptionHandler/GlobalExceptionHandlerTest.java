package dev.luhwani.cookieLoginApi.exceptionHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.luhwani.cookieLoginApi.customExceptions.AuthInfrastructureException;
import dev.luhwani.cookieLoginApi.customExceptions.DuplicateEmailException;
import dev.luhwani.cookieLoginApi.customExceptions.DuplicateUsernameException;
import dev.luhwani.cookieLoginApi.dto.*;
import dev.luhwani.cookieLoginApi.security.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for {@link GlobalExceptionHandler}.
 *
 * Uses the full Spring context (via the test profile) so we can exercise the
 * exception handler through real HTTP requests, which is the only path that
 * triggers @RestControllerAdvice.
 *
 * Each exception type is triggered by sending a request that causes the
 * corresponding exception to be thrown inside the application layer.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("GlobalExceptionHandler — Integration Tests")
class GlobalExceptionHandlerTest {

    @Autowired private MockMvc      mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final ObjectMapper mapper = new ObjectMapper();

    // ── Validation errors (MethodArgumentNotValidException) ──

    @Nested
    @DisplayName("Validation errors → 400 Bad Request")
    class ValidationErrors {

        @Test
        @DisplayName("Invalid email triggers 400 with error map")
        void invalidEmail_400_withErrors() throws Exception {
            MvcResult result = mockMvc.perform(post("/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"not-an-email\",\"username\":\"validname\",\"password\":\"Password1\"}")
                    .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            JsonNode body = mapper.readTree(result.getResponse().getContentAsString());
            assertThat(body.path("success").asBoolean()).isFalse();
            assertThat(body.path("error").path("status").asInt()).isEqualTo(400);
            // errors map contains the field name "email"
            assertThat(body.path("error").path("errors").has("email")).isTrue();
        }

        @Test
        @DisplayName("Multiple validation failures → all fields present in error map")
        void multipleViolations_allFieldsInErrorMap() throws Exception {
            MvcResult result = mockMvc.perform(post("/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"bad\",\"username\":\"ab\",\"password\":\"weak\"}")
                    .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            JsonNode errors = mapper.readTree(result.getResponse().getContentAsString())
                    .path("error").path("errors");

            // At least email and password should be flagged (username might also be)
            assertThat(errors.has("email")).isTrue();
            assertThat(errors.has("password")).isTrue();
        }

        @Test
        @DisplayName("Response content-type is application/json even for validation errors")
        void validationError_contentTypeIsJson() throws Exception {
            mockMvc.perform(post("/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"bad\"}")
                    .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }
    }

    // ── DuplicateEmailException → 409 ──

    @Nested
    @DisplayName("DuplicateEmailException → 409 Conflict")
    class DuplicateEmail {

        @Test
        @DisplayName("Registering the same email twice → 409")
        void duplicateEmail_returns409() throws Exception {
            mockMvc.perform(post("/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"dupe_eh@example.com\",\"username\":\"dupeuser1\",\"password\":\"Password1\"}")
                    .with(csrf()))
                    .andExpect(status().isCreated());

            MvcResult result = mockMvc.perform(post("/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"dupe_eh@example.com\",\"username\":\"dupeuser2\",\"password\":\"Password1\"}")
                    .with(csrf()))
                    .andExpect(status().isConflict())
                    .andReturn();

            JsonNode body = mapper.readTree(result.getResponse().getContentAsString());
            assertThat(body.path("success").asBoolean()).isFalse();
            assertThat(body.path("error").path("status").asInt()).isEqualTo(409);
            assertThat(body.path("error").path("errors").path("message").asText())
                    .containsIgnoringCase("email");

            // cleanup
            jdbc.update("DELETE FROM authorities WHERE user_id = (SELECT id FROM users WHERE email = 'dupe_eh@example.com')");
            jdbc.update("DELETE FROM users WHERE email = 'dupe_eh@example.com'");
        }
    }

    // ── DuplicateUsernameException → 409 ─

    @Nested
    @DisplayName("DuplicateUsernameException → 409 Conflict")
    class DuplicateUsername {

        @Test
        @DisplayName("Registering the same username twice → 409")
        void duplicateUsername_returns409() throws Exception {
            mockMvc.perform(post("/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"dup_u1@example.com\",\"username\":\"dupename\",\"password\":\"Password1\"}")
                    .with(csrf()))
                    .andExpect(status().isCreated());

            MvcResult result = mockMvc.perform(post("/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"dup_u2@example.com\",\"username\":\"dupename\",\"password\":\"Password1\"}")
                    .with(csrf()))
                    .andExpect(status().isConflict())
                    .andReturn();

            JsonNode body = mapper.readTree(result.getResponse().getContentAsString());
            assertThat(body.path("error").path("errors").path("message").asText())
                    .containsIgnoringCase("username");

            // cleanup
            jdbc.update("DELETE FROM authorities WHERE user_id IN (SELECT id FROM users WHERE email IN ('dup_u1@example.com','dup_u2@example.com'))");
            jdbc.update("DELETE FROM users WHERE email IN ('dup_u1@example.com','dup_u2@example.com')");
        }
    }

    // ── AuthenticationCredentialsNotFoundException → 400 ──

    @Nested
    @DisplayName("AuthenticationCredentialsNotFoundException → 400 Bad Request")
    class MissingCredentials {

        @Test
        @DisplayName("Login with no email param → 400 (thrown by LoginRateLimiter, handled globally)")
        void loginWithNoEmail_returns400() throws Exception {
            MvcResult result = mockMvc.perform(post("/login")
                    .param("password", "Password1")
                    .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            JsonNode body = mapper.readTree(result.getResponse().getContentAsString());
            assertThat(body.path("success").asBoolean()).isFalse();
            assertThat(body.path("error").path("status").asInt()).isEqualTo(400);
        }
    }

    // ── General ApiResponse shape ──

    @Test
    @DisplayName("All error responses wrap error in ApiResponse with success=false")
    void allErrors_wrappedInApiResponse() throws Exception {
        // Use a validation error as a representative case
        MvcResult result = mockMvc.perform(post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"bad\"}")
                .with(csrf()))
                .andReturn();

        JsonNode body = mapper.readTree(result.getResponse().getContentAsString());
        // All responses must follow the ApiResponse shape
        assertThat(body.has("success")).isTrue();
        assertThat(body.has("error")).isTrue();
        assertThat(body.path("success").asBoolean()).isFalse();
    }
}