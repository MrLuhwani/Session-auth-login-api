package dev.luhwani.cookieLoginApi.controllers;

import dev.luhwani.cookieLoginApi.security.CustomUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class MeController {

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal CustomUserPrincipal principal) {
        return Map.of(
                "id", principal.getId(),
                "email", principal.getEmail(),
                "username", principal.getDisplayUsername()
        );
    }

    /*
    maybe validate the principal as it comes
    if (principal == null) {
    throw new UnauthorizedException(...);
}
     */
}

/*
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
                principal.getId(),
                principal.getEmail(),
                principal.getDisplayUsername(),
                principal.getAuthorities().stream()
                        .map(a -> a.getAuthority())
                        .collect(Collectors.toList())
        );
    }
} */