package dev.luhwani.cookieLoginApi.controllers;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import dev.luhwani.cookieLoginApi.customExceptions.BadRequestException;
import dev.luhwani.cookieLoginApi.dto.RegisterRequest;
import dev.luhwani.cookieLoginApi.dto.RegisterResponse;
import dev.luhwani.cookieLoginApi.security.CustomUserPrincipal;
import dev.luhwani.cookieLoginApi.services.RegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @RequestBody RegisterRequest req, HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        if (req == null) {
            throw new BadRequestException("RequestBody is required");
        }
        Authentication authentication = registrationService.registerAndLogin(req, httpRequest, httpResponse);
        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();

        RegisterResponse body = new RegisterResponse(
                "Registration successful",
                principal.getEmail(),
                principal.getDisplayUsername(),
                principal.getAuthorities().stream()
                        .map(a -> a.getAuthority())
                        .collect(Collectors.toList()),
                "/me");

        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

}
