package dev.luhwani.cookieLoginApi.controllers;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import dev.luhwani.cookieLoginApi.customExceptions.AuthInfrastructureException;
import dev.luhwani.cookieLoginApi.dto.ApiResponse;
import dev.luhwani.cookieLoginApi.dto.RegisterRequest;
import dev.luhwani.cookieLoginApi.dto.RegisterResponse;
import dev.luhwani.cookieLoginApi.dto.Role;
import dev.luhwani.cookieLoginApi.security.CustomUserPrincipal;
import dev.luhwani.cookieLoginApi.services.RegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
public class RegistrationController {

    private final RegistrationService registrationService;
    private static final Logger log = LoggerFactory.getLogger(RegistrationController.class);

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(
            @Valid @RequestBody RegisterRequest req, HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        
        log.debug("Register request from ip: {}", httpRequest.getRemoteAddr());

        Role user = Role.USER;
        Optional <Authentication> authentication = registrationService.registerUser(req, user, httpRequest, httpResponse);
        if (authentication.isEmpty()) {
            log.warn("Empty optional while retrieving authentication object of: {} from ip {}", req.email(), httpRequest.getLocalAddr());
            throw new AuthInfrastructureException("Could not authenticate user");
        }
        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.get().getPrincipal();
        
        RegisterResponse data = new RegisterResponse(
                principal.getEmail(),
                principal.getDisplayUsername(),
                principal.getAuthorities().stream()
                        .map(a -> a.getAuthority())
                        .collect(Collectors.toList()));

        ApiResponse<RegisterResponse> response = new ApiResponse<RegisterResponse>(true, data,"Registration successful", Map.of("redirect","http://localhost:8080/me"));

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
