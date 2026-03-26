package dev.luhwani.cookieLoginApi.services;

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
    public Authentication registerAndLogin(RegisterRequest req, HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        Long userId = register(req);
        Authentication authentication = loginAfterRegistration(req, httpRequest, httpResponse);
        userRepo.setLastLogin(userId);
        return authentication;
    }

    public Long register(RegisterRequest req) {

        //trying to avoid this check by adding annotations to the rest controller
        //and to the dto

        //try and do something like a compare with common passwords like password, or something else

        try {
            String passwordHash = passwordEncoder.encode(req.password());
            Long userId = userRepo.registerUserAndReturnId(req, passwordHash);
            return userId;
        } catch (DataIntegrityViolationException e) {
            String message = e.getMostSpecificCause().getMessage().toLowerCase();
            if (message.contains("users_username_key") || message.contains("unique")) {
                throw new DuplicateUsernameException("Username is already taken");
            } else if (message.contains("users_email_key") || message.contains("unique")) {
                throw new DuplicateEmailException("Email is already registered");
            } else {
                throw new RuntimeException("A database error occurred during registration", e);
            }
        } catch (DataAccessException e) {

            log.error("Error while registering {}", req.email(), e);

            throw new AuthInfrastructureException(e.getMessage(), e);    
        }
    }

    public Authentication loginAfterRegistration(RegisterRequest req,
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
