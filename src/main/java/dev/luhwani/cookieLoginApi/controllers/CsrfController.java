package dev.luhwani.cookieLoginApi.controllers;

import java.util.Map;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.luhwani.cookieLoginApi.dto.ApiResponse;

@RestController
public class CsrfController {

    @GetMapping("/csrf")
    public ApiResponse<Map<String, String>> csrf(CsrfToken token) {
        ApiResponse<Map<String, String>> response = new ApiResponse<>(
                true,
                Map.of("token", token.getToken())
        );
        return response;
    }
}