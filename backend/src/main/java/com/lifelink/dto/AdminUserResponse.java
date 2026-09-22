package com.lifelink.dto;

import com.lifelink.entity.User;
import com.lifelink.enums.BloodGroup;
import com.lifelink.enums.Role;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        String name,
        String email,
        String phone,
        Role role,
        BloodGroup bloodGroup,
        String district,
        boolean available,
        LocalDate lastDonationDate,
        LocalDateTime createdAt
) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getBloodGroup(),
                user.getDistrict(),
                user.isAvailable(),
                user.getLastDonationDate(),
                user.getCreatedAt()
        );
    }
}
