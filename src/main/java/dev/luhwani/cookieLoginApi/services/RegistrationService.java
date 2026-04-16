package dev.luhwani.cookieLoginApi.services;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.luhwani.cookieLoginApi.customExceptions.AuthInfrastructureException;
import dev.luhwani.cookieLoginApi.customExceptions.DuplicateEmailException;
import dev.luhwani.cookieLoginApi.customExceptions.DuplicateUsernameException;
import dev.luhwani.cookieLoginApi.dto.RegisterRequest;
import dev.luhwani.cookieLoginApi.dto.Role;
import dev.luhwani.cookieLoginApi.repositories.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class RegistrationService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final SecurityContextRepository securityContextRepository;
    private final AuthenticationManager authenticationManager;

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);

    public RegistrationService(UserRepository userRepo,
            SecurityContextRepository securityContextRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.securityContextRepository = securityContextRepository;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public Optional<Authentication> registerUser(RegisterRequest req, Role role, HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        Long userId = register(req, role);
        if (role.getAuthority().equals("ROLE_ADMIN")) {
            return Optional.empty();
        }
        Authentication authentication = loginAfterRegistration(req, httpRequest, httpResponse);
        userRepo.setLastLogin(userId);
        return Optional.of(authentication);
    }

    private Long register(RegisterRequest req, Role role) {

        try {
            String passwordHash = passwordEncoder.encode(req.password());
            Long userId = userRepo.registerUserAndReturnId(req, passwordHash, role);
            return userId;
        } catch (DataIntegrityViolationException e) {
            String message = e.getMostSpecificCause().getMessage().toLowerCase();
            if (message.contains("users_username_key")) {
                throw new DuplicateUsernameException("Username is already taken");
            } else if (message.contains("users_email_key")) {
                throw new DuplicateEmailException("Email is already registered");
            } else {
                throw new RuntimeException("A database error occurred during registration", e);
            }
        } catch (DataAccessException e) {

            log.error("Error while registering {}", req.email(), e);

            throw new AuthInfrastructureException(e.getMessage(), e);    
        }
    }

    private Authentication loginAfterRegistration(RegisterRequest req,
            HttpServletRequest request,
            HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(req.email().trim(), req.password()));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        return authentication;
    }

}
