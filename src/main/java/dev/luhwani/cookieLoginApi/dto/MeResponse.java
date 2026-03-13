package dev.luhwani.cookieLoginApi.dto;

import java.util.List;

public record MeResponse(
                String email,
                String username,
                List<String> authorities) {
}