package dev.luhwani.cookieLoginApi.services;

import dev.luhwani.cookieLoginApi.customExceptions.AuthInfrastructureException;
import dev.luhwani.cookieLoginApi.customExceptions.DuplicateEmailException;
import dev.luhwani.cookieLoginApi.customExceptions.DuplicateUsernameException;
import dev.luhwani.cookieLoginApi.dto.RegisterRequest;
import dev.luhwani.cookieLoginApi.dto.Role;
import dev.luhwani.cookieLoginApi.repositories.UserRepository;
import dev.luhwani.cookieLoginApi.security.CustomUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RegistrationService}.
 *
 * Mocks all collaborators — zero DB, zero Spring context.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RegistrationService")
class RegistrationServiceTest {

    @Mock private UserRepository         userRepo;
    @Mock private PasswordEncoder        passwordEncoder;
    @Mock private SecurityContextRepository secCtxRepo;
    @Mock private AuthenticationManager  authManager;
    @Mock private HttpServletRequest     httpReq;
    @Mock private HttpServletResponse    httpResp;
    @Mock private Authentication         authentication;
    @Mock private CustomUserPrincipal    principal;

    private RegistrationService service;

    private static final RegisterRequest VALID_REQ =
            new RegisterRequest("user@example.com", "johndoe", "Password1");

    @BeforeEach
    void setUp() {
        service = new RegistrationService(userRepo, secCtxRepo, passwordEncoder, authManager);
        SecurityContextHolder.clearContext();
    }

    // ── USER role registration

    @Nested
    @DisplayName("USER role registration")
    class UserRoleRegistration {

        @BeforeEach
        void stubHappyPath() {
            when(passwordEncoder.encode(anyString())).thenReturn("$hashed");
            when(userRepo.registerUserAndReturnId(any(), anyString(), any())).thenReturn(1L);
            when(authManager.authenticate(any())).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(principal);
        }

        @Test
        @DisplayName("returns a non-empty Optional<Authentication>")
        void returnsNonEmptyOptional() {
            Optional<Authentication> result = service.registerUser(VALID_REQ, Role.USER, httpReq, httpResp);
            assertThat(result).isPresent().contains(authentication);
        }

        @Test
        @DisplayName("calls setLastLogin after successful auth")
        void callsSetLastLogin() {
            service.registerUser(VALID_REQ, Role.USER, httpReq, httpResp);
            verify(userRepo).setLastLogin(1L);
        }

        @Test
        @DisplayName("saves the SecurityContext to the repository")
        void savesSecurityContext() {
            service.registerUser(VALID_REQ, Role.USER, httpReq, httpResp);
            verify(secCtxRepo).saveContext(any(), eq(httpReq), eq(httpResp));
        }

        @Test
        @DisplayName("authenticates with the plaintext password (not the hash)")
        void authenticatesWithPlaintextPassword() {
            service.registerUser(VALID_REQ, Role.USER, httpReq, httpResp);
            ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
                    ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
            verify(authManager).authenticate(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("user@example.com");
            assertThat(captor.getValue().getCredentials()).isEqualTo("Password1");
        }

        @Test
        @DisplayName("stores the ENCODED password in the DB — never the plaintext")
        void storesEncodedPassword() {
            service.registerUser(VALID_REQ, Role.USER, httpReq, httpResp);
            verify(userRepo).registerUserAndReturnId(eq(VALID_REQ), eq("$hashed"), eq(Role.USER));
        }

        @Test
        @DisplayName("email is trimmed before use (leading/trailing spaces removed)")
        void emailIsTrimmedForAuth() {
            RegisterRequest reqWithSpaces = new RegisterRequest(" user@example.com ", "johndoe", "Password1");
            service.registerUser(reqWithSpaces, Role.USER, httpReq, httpResp);
            ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
                    ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
            verify(authManager).authenticate(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("user@example.com");
        }
    }

    // ── ADMIN role registration

    @Nested
    @DisplayName("ADMIN role registration")
    class AdminRoleRegistration {

        @Test
        @DisplayName("returns an EMPTY Optional — admin does not auto-login after registration")
        void returnsEmptyOptional() {
            when(passwordEncoder.encode(anyString())).thenReturn("$hashed");
            when(userRepo.registerUserAndReturnId(any(), anyString(), any())).thenReturn(2L);

            Optional<Authentication> result = service.registerUser(VALID_REQ, Role.ADMIN, httpReq, httpResp);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("does NOT call authenticationManager — no auto-login for admin")
        void noAutoLoginForAdmin() {
            when(passwordEncoder.encode(anyString())).thenReturn("$hashed");
            when(userRepo.registerUserAndReturnId(any(), anyString(), any())).thenReturn(2L);

            service.registerUser(VALID_REQ, Role.ADMIN, httpReq, httpResp);

            verifyNoInteractions(authManager);
        }

        @Test
        @DisplayName("does NOT call setLastLogin for admin registration")
        void noSetLastLoginForAdmin() {
            when(passwordEncoder.encode(anyString())).thenReturn("$hashed");
            when(userRepo.registerUserAndReturnId(any(), anyString(), any())).thenReturn(2L);

            service.registerUser(VALID_REQ, Role.ADMIN, httpReq, httpResp);

            verify(userRepo, never()).setLastLogin(any());
        }
    }

    // ── Exception handling

    @Nested
    @DisplayName("Exception handling")
    class ExceptionHandling {

        @BeforeEach
        void stubEncoder() {
            when(passwordEncoder.encode(anyString())).thenReturn("$hashed");
        }

        @Test
        @DisplayName("DataIntegrityViolation on username key → DuplicateUsernameException")
        void duplicateUsername_throws() {
            when(userRepo.registerUserAndReturnId(any(), anyString(), any()))
                    .thenThrow(new DataIntegrityViolationException("users_username_key constraint"));
            assertThatThrownBy(() -> service.registerUser(VALID_REQ, Role.USER, httpReq, httpResp))
                    .isInstanceOf(DuplicateUsernameException.class)
                    .hasMessageContaining("Username is already taken");
        }

        @Test
        @DisplayName("DataIntegrityViolation on email key → DuplicateEmailException")
        void duplicateEmail_throws() {
            when(userRepo.registerUserAndReturnId(any(), anyString(), any()))
                    .thenThrow(new DataIntegrityViolationException("users_email_key constraint"));
            assertThatThrownBy(() -> service.registerUser(VALID_REQ, Role.USER, httpReq, httpResp))
                    .isInstanceOf(DuplicateEmailException.class)
                    .hasMessageContaining("Email is already registered");
        }

        @Test
        @DisplayName("generic DataAccessException → AuthInfrastructureException")
        void dataAccessException_wrappedInInfrastructureException() {
            when(userRepo.registerUserAndReturnId(any(), anyString(), any()))
                    .thenThrow(new EmptyResultDataAccessException(1));
            assertThatThrownBy(() -> service.registerUser(VALID_REQ, Role.USER, httpReq, httpResp))
                    .isInstanceOf(AuthInfrastructureException.class);
        }

        @Test
        @DisplayName("when DB throws, authManager is never called — no half-baked auth")
        void dbFailure_noAuthManagerCall() {
            when(userRepo.registerUserAndReturnId(any(), anyString(), any()))
                    .thenThrow(new DataIntegrityViolationException("users_username_key"));
            try {
                service.registerUser(VALID_REQ, Role.USER, httpReq, httpResp);
            } catch (DuplicateUsernameException ignored) {}
            verifyNoInteractions(authManager);
        }
    }
}