package com.lifelink.dto;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInSeconds,
        UserProfileResponse user
) {
}
