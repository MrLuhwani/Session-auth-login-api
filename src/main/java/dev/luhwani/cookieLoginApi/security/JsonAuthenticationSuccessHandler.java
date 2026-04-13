package dev.luhwani.cookieLoginApi.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.luhwani.cookieLoginApi.dto.ApiResponse;
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

        
        userRepository.setLastLogin(principal.getId());
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        ApiResponse<Map<String, Object>> body = new ApiResponse<>(true, Map.of(
                "id", principal.getId(),
                "user", Map.of(
                        "username", principal.getUsername(),
                "roles", principal.getAuthorities().stream()
                        .map(auth -> auth.getAuthority())
                        .collect(Collectors.toList())
                )
        ), "Login successful", Map.of("redirect", "http://localhost:8080/me"));
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
