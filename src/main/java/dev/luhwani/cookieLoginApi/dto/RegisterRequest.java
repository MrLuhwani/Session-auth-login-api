package dev.luhwani.cookieLoginApi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        //check later if the annotation for email check is too simple
        @Email(message = "Please provide a valid email address")
        @Size(min = 5, max = 50, message = "Email is to be within {min} and {max}")
        String email,

        @NotBlank(message = "Username is required")
        @Size(min = 5, max = 20, message = "Username is to be within {min} and {max}")
        String username,

        //at least a lowercase
        //at least an uppercase
        //at least one digit
        //length of 8-30
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,30}$", message = "Invalid password format")
        String password) {
}
