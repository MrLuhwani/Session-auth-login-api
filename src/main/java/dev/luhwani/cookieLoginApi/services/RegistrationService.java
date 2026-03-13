package dev.luhwani.cookieLoginApi.services;

import org.springframework.dao.DataAccessException;
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
import dev.luhwani.cookieLoginApi.customExceptions.BadRequestException;
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
    public Long register(RegisterRequest req) {
        String email = req.email() == null ? null : req.email().trim().toLowerCase();
        String username = req.username() == null ? null : req.username().trim();
        String password = req.password() == null ? null : req.password();
        if (!Utils.validEmail(email)) {
            throw new BadRequestException("Invalid email format");
        }

        if (!Utils.validUsername(username)) {
            throw new BadRequestException("Invalid username format");
        }

        if (!Utils.validPassword(password)) {
            throw new BadRequestException("Invalid password format");
        }

        try {
            if (userRepo.emailExists(email)) {
                throw new DuplicateEmailException("This account already exists");
            }
            if (userRepo.usernameExists(username)) {
                throw new DuplicateUsernameException("Username is already in use");
            }
            String passwordHash = passwordEncoder.encode(req.password());
            Long userId = userRepo.registerUserAndReturnId(req, passwordHash);
            return userId;
        } catch (DataAccessException e) {
            throw new AuthInfrastructureException("Error occured while registering user", e);
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

    @Transactional
    public Authentication registerAndLogin(RegisterRequest req, HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        Long userId = register(req);
        Authentication authentication = loginAfterRegistration(req, httpRequest, httpResponse);
        userRepo.setLastLogin(userId);
        return authentication;
    }
}
