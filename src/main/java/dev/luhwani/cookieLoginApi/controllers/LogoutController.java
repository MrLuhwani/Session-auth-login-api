package dev.luhwani.cookieLoginApi.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.luhwani.cookieLoginApi.dto.LogoutResponse;
import dev.luhwani.cookieLoginApi.services.LogoutService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
public class LogoutController {

    private final LogoutService logoutService;

    public LogoutController(LogoutService logoutService) {
        this.logoutService = logoutService;
    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        logoutService.logout(request, response);

        return ResponseEntity.ok(
                new LogoutResponse(
                        "Logout successful",
                        "/login"
                )
        );
    }
}