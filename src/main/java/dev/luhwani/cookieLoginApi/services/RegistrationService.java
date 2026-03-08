package dev.luhwani.cookieLoginApi.services;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
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
import dev.luhwani.cookieLoginApi.customExceptions.UnknownDBException;
import dev.luhwani.cookieLoginApi.dto.RegisterRequest;
import dev.luhwani.cookieLoginApi.repositories.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class RegistrationService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public RegistrationService(UserRepository userRepo,
            SecurityContextRepository securityContextRepository, AuthenticationManager authenticationManager, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @Transactional
    public Long registerAndLogin(RegisterRequest req, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
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
                throw new DuplicateEmailException("Email is already in use");
            }
            if (userRepo.usernameExists(username)) {
                throw new DuplicateUsernameException("Username is already in use");
            }
            String passwordHash = passwordEncoder.encode(req.password());
            Long userId = userRepo.registerUserAndReturnId(req, passwordHash);
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(req.email().trim(),
                            req.password()));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, httpRequest, httpResponse);
            return userId;
        } catch (DataAccessException e) {
            if (e instanceof BadSqlGrammarException || e instanceof CannotGetJdbcConnectionException) {
                throw new AuthInfrastructureException();
            } else {
                throw new UnknownDBException("Database error while registering user", e);
            }
        }
    }
}

/*
    @Transactional
    public Authentication registerAndLogin(
            RegisterRequest req,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        try {
            
            userRepository.createUser(new RegisterRequest(email, username, password), passwordHash);

            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email, password)
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, request, response);

            return authentication;
    }
*/