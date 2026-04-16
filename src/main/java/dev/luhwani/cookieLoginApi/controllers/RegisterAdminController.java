package dev.luhwani.cookieLoginApi.controllers;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import dev.luhwani.cookieLoginApi.dto.ApiResponse;
import dev.luhwani.cookieLoginApi.dto.RegisterRequest;
import dev.luhwani.cookieLoginApi.dto.RegisterResponse;
import dev.luhwani.cookieLoginApi.dto.Role;
import dev.luhwani.cookieLoginApi.services.RegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@Controller
public class RegisterAdminController {

    /*
     * for someone to register an admin, they need to be an admin themselves. So
     * this endpoint should
     * be protected by the same auth as the other admin endpoints.
     */

    private final RegistrationService registrationService;

    public RegisterAdminController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    private static final Logger log = LoggerFactory.getLogger(RegisterAdminController.class);

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> registerAdmin(
            @Valid @RequestBody RegisterRequest req, HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        log.debug("Admin registration request from ip: {}", httpRequest.getRemoteAddr());

        Role admin = Role.ADMIN;
        registrationService.registerUser(req, admin, httpRequest, httpResponse);
        RegisterResponse data = new RegisterResponse(
                req.email(), req.username(), List.of("ROLE_ADMIN"));
        ApiResponse<RegisterResponse> response = new ApiResponse<RegisterResponse>(true, data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);

    }

}
