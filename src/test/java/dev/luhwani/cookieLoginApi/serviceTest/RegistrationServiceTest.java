package dev.luhwani.cookieLoginApi.serviceTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;

import dev.luhwani.cookieLoginApi.customExceptions.AuthInfrastructureException;
import dev.luhwani.cookieLoginApi.customExceptions.DuplicateUsernameException;
import dev.luhwani.cookieLoginApi.dto.RegisterRequest;
import dev.luhwani.cookieLoginApi.repositories.UserRepository;
import dev.luhwani.cookieLoginApi.services.RegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
public class RegistrationServiceTest {

    @Mock
    private UserRepository userRepo;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SecurityContextRepository securityContextRepository;
    @Mock
    private AuthenticationManager authenticationManager;
    @InjectMocks
    private RegistrationService registrationService;

    @Test
    void register_shouldReturnUserId() {
        RegisterRequest request = new RegisterRequest("emailTest@gmail.com", "testUser", "password");
        when(passwordEncoder.encode(request.password())).thenReturn("hashed");
        when(userRepo.registerUserAndReturnId(request, "hashed")).thenReturn(1L);
        Long result = registrationService.register(request);
        assertEquals(1L, result);
    }

    
    @Test
    void shouldRegisterUserSuccessfully() {
        RegisterRequest req = new RegisterRequest("test@mail.com", "user123", "Password1");

        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepo.registerUserAndReturnId(any(), any())).thenReturn(1L);

        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(auth);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        Authentication result = registrationService.registerAndLogin(req, request, response);

        assertNotNull(result);
        verify(userRepo).setLastLogin(1L);
    }


    @Test
    void register_shouldThrowDuplicateUsernameException() {
        RegisterRequest request = new RegisterRequest("emailTest@gmail.com", "testUser", "password");
        when(passwordEncoder.encode(request.password())).thenReturn("hashed");
        DataIntegrityViolationException ex = new DataIntegrityViolationException("error",
                new RuntimeException("users_username_key"));
        when(userRepo.registerUserAndReturnId(request, "hashed")).thenThrow(ex);
        assertThrows(DuplicateUsernameException.class, () -> {
            registrationService.register(request);
        });
    }

    @Test
    void register_shouldThrowDuplicateEmailException() {
        RegisterRequest request = new RegisterRequest("emailTest@gmail.com", "testUser", "password");
        when(passwordEncoder.encode(request.password())).thenReturn("hashed");
        DataIntegrityViolationException ex = new DataIntegrityViolationException("error",
                new RuntimeException("users_email_key"));
        when(userRepo.registerUserAndReturnId(request, "hashed")).thenThrow(ex);
        assertThrows(DuplicateUsernameException.class, () -> {
            registrationService.register(request);
        });
    }

    @Test
    void register_shouldThrowInfrastructureException_onDataAccessError() {
        RegisterRequest req = new RegisterRequest("user", "email@test.com", "password");
        when(passwordEncoder.encode("password")).thenReturn("hashed");
        when(userRepo.registerUserAndReturnId(req, "hashed"))
                .thenThrow(new DataAccessException("DB down") {
                });
        assertThrows(AuthInfrastructureException.class, () -> {
            registrationService.register(req);
        });
    }

    @Test
    void loginAfterRegistration_shouldAuthenticateAndSaveContext() {
        RegisterRequest req = new RegisterRequest("user", "email@test.com", "password");
        Authentication mockAuth = mock(Authentication.class);
        when(authenticationManager.authenticate(any()))
                .thenReturn(mockAuth);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        Authentication result = registrationService.loginAfterRegistration(req, request, response);

        assertEquals(mockAuth, result);

        verify(securityContextRepository)
                .saveContext(any(SecurityContext.class), eq(request), eq(response));
    }

    @Test
    void registerAndLogin_shouldCallAllSteps() {
        RegisterRequest req = new RegisterRequest("user", "email@test.com", "password");

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(passwordEncoder.encode("password")).thenReturn("hashed");
        when(userRepo.registerUserAndReturnId(req, "hashed")).thenReturn(1L);

        Authentication mockAuth = mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(mockAuth);

        Authentication result = registrationService.registerAndLogin(req, request, response);

        assertEquals(mockAuth, result);

        verify(userRepo).setLastLogin(1L);
    }
}
