package dev.luhwani.cookieLoginApi.controllers;

import dev.luhwani.cookieLoginApi.dto.MeResponse;
import dev.luhwani.cookieLoginApi.security.CustomUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
public class MeController {

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal CustomUserPrincipal principal) {
        return new MeResponse(
                principal.getEmail(),
                principal.getDisplayUsername(),
                principal.getAuthorities().stream()
                        .map(a -> a.getAuthority())
                        .collect(Collectors.toList())
        );
    }
}
