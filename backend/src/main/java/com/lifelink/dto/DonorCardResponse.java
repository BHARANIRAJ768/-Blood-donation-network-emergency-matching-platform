package com.lifelink.dto;

import com.lifelink.entity.User;
import com.lifelink.enums.BloodGroup;

import java.time.LocalDate;

public record DonorCardResponse(
        Long id,
        String name,
        String photo,
        BloodGroup bloodGroup,
        String district,
        Double distanceKm,
        LocalDate lastDonationDate
) {
    public static DonorCardResponse from(User donor, Double distanceKm) {
        return new DonorCardResponse(
                donor.getId(),
                donor.getName(),
                donor.getPhoto(),
                donor.getBloodGroup(),
                donor.getDistrict(),
                distanceKm,
                donor.getLastDonationDate()
        );
    }
}
