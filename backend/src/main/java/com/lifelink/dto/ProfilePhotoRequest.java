package com.lifelink.dto;

import jakarta.validation.constraints.NotBlank;

public record ProfilePhotoRequest(
        @NotBlank(message = "Photo data is required")
        String photo
) {
}
