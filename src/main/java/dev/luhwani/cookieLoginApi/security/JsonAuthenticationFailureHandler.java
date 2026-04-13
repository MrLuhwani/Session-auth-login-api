package dev.luhwani.cookieLoginApi.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;

import dev.luhwani.cookieLoginApi.dto.ApiResponse;
import dev.luhwani.cookieLoginApi.dto.ErrorResponse;
import dev.luhwani.cookieLoginApi.repositories.UserRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JsonAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private Cache<String, String> lockedAccountsToIp;

    private static final Map<String, Integer> emailToAttempts = new ConcurrentHashMap<>();

    private static final Logger log = LoggerFactory.getLogger(JsonAuthenticationFailureHandler.class);

    private static final short MAX_ATTEMPTS = 7;

    public JsonAuthenticationFailureHandler(ObjectMapper objectMapper, UserRepository userRepository,
            Cache<String, String> lockedAccountsToIp) {
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.lockedAccountsToIp = lockedAccountsToIp;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {

        String email = request.getParameter("email");
        String ip = request.getRemoteAddr();

        if (exception instanceof LockedException) {
            // there's meant to be code here, telling the user the account is locked
            // but I'm trying to prevent user enumeration
            log.warn("IP {} attempted access on already-locked account: {}", ip, email);

            if (email != null && !email.isBlank() && lockedAccountsToIp.getIfPresent(email) == null) {
                lockedAccountsToIp.put(email, ip);
            }

            sendGenericFailureResponse(response);
            return;
        }

        if (email == null) {
            log.warn("Null email request sent by ip: {}", ip);
            sendGenericFailureResponse(response);
            return;
        }

        if (!email.isBlank()) {

            emailToAttempts.merge(email, 1, Integer::sum);

            if (emailToAttempts.get(email) >= MAX_ATTEMPTS) {
                userRepository.tempAcctLock(email, LocalDateTime.now().plusHours(3));
                log.warn("Email: {} has been temporarily locked", email);
                lockedAccountsToIp.put(email, ip);

                // Clean up attempt counter, no longer needed
                emailToAttempts.remove(email);

            }
        }
        sendGenericFailureResponse(response);
    }

    private void sendGenericFailureResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.addHeader("error", "AuthenticationException");
        ErrorResponse error = new ErrorResponse(401, Map.of("message", "Invalid email or password"));
        ApiResponse<ErrorResponse> msg = new ApiResponse<>(false, error);
        objectMapper.writeValue(response.getOutputStream(), msg);
    }
}
