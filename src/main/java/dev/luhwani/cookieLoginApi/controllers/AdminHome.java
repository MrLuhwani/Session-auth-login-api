package dev.luhwani.cookieLoginApi.controllers;

import org.springframework.web.bind.annotation.RestController;

import dev.luhwani.cookieLoginApi.dto.ApiResponse;
import dev.luhwani.cookieLoginApi.dto.MeResponse;
import dev.luhwani.cookieLoginApi.security.CustomUserPrincipal;

import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
public class AdminHome {

    @GetMapping("/admin/home")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<MeResponse> adminHome(@AuthenticationPrincipal CustomUserPrincipal principalAdmin) {
        var meResponse = new MeResponse(
                principalAdmin.getEmail(),
                principalAdmin.getDisplayUsername(),
                principalAdmin.getAuthorities().stream()
                        .map(a -> a.getAuthority())
                        .collect(Collectors.toList())
        );
        return new ApiResponse<>(true, meResponse);
    }
    
}
