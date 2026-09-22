package com.lifelink.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForgotPasswordRequest(
        @NotBlank(message = "Email is required")
        @jakarta.validation.constraints.Email(message = "Please provide a valid email address")
        String email
) {
}
