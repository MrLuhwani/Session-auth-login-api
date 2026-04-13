package dev.luhwani.cookieLoginApi.security;

import java.io.IOException;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.luhwani.cookieLoginApi.dto.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JsonLogoutSuccessHandler implements LogoutSuccessHandler {

    private final ObjectMapper objectMapper;

    public JsonLogoutSuccessHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void onLogoutSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        ApiResponse<String> msg = new ApiResponse<>(true, "Logout Successful", Map.of("redirect","http://localhost:8080/login"));
        objectMapper.writeValue(response.getOutputStream(), msg);
    }
    
}
