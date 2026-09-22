package com.lifelink.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 120, message = "Name must be at most 120 characters")
        String name,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^[+]?[0-9 ]{7,15}$", message = "Please provide a valid phone number")
        String phone,

        String bloodGroup,

        String district,

        Double latitude,

        Double longitude,

        boolean available,

        String photo
) {
}
