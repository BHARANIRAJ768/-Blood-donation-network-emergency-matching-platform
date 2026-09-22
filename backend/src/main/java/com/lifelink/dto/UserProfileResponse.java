package com.lifelink.dto;

import com.lifelink.entity.User;
import com.lifelink.enums.BloodGroup;
import com.lifelink.enums.Role;

import java.time.LocalDate;

public record UserProfileResponse(
        Long id,
        String name,
        String email,
        String phone,
        Role role,
        BloodGroup bloodGroup,
        String district,
        boolean available,
        LocalDate lastDonationDate,
        String photo,
        Double latitude,
        Double longitude
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getBloodGroup(),
                user.getDistrict(),
                user.isAvailable(),
                user.getLastDonationDate(),
                user.getPhoto(),
                user.getLatitude(),
                user.getLongitude()
        );
    }
}
