package dev.luhwani.cookieLoginApi.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.luhwani.cookieLoginApi.dto.ApiResponse;
import dev.luhwani.cookieLoginApi.dto.ErrorResponse;

import java.io.IOException;
import java.util.Map;

@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JsonAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        ErrorResponse error = new ErrorResponse(401, Map.of("message", "Authentication required"));
        ApiResponse<ErrorResponse> msg = new ApiResponse<>(false, error);
        objectMapper.writeValue(response.getOutputStream(), msg);
    }
}
