package com.lifelink.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 120, message = "Name must be at most 120 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email address")
        @Size(max = 190, message = "Email must be at most 190 characters")
        String email,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^[+]?[0-9 ]{7,15}$", message = "Please provide a valid phone number")
        String phone,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be at least 8 characters")
        String password,

        @NotBlank(message = "Role is required")
        String role,

        String bloodGroup,

        String district,

        Double latitude,

        Double longitude,

        boolean available
) {
}
