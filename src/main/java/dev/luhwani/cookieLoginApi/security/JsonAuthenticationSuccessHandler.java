package dev.luhwani.cookieLoginApi.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.luhwani.cookieLoginApi.repositories.UserRepository;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class JsonAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;

    public JsonAuthenticationSuccessHandler(ObjectMapper objectMapper, UserRepository userRepository) {
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();

        Map<String, Object> body = Map.of(
                "message", "Login successful",
                "user", Map.of(
                        "id", principal.getId(),
                        "email", principal.getEmail(),
                        "username", principal.getDisplayUsername(),
                        "authorities", principal.getAuthorities().stream()
                                .map(a -> a.getAuthority())
                                .collect(Collectors.toList())
                )
        );
        userRepository.setLastLogin(principal.getId());
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
