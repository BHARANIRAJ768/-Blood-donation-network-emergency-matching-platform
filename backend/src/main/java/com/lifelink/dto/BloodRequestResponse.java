package com.lifelink.dto;

import com.lifelink.entity.BloodRequest;
import com.lifelink.enums.BloodGroup;
import com.lifelink.enums.RequestStatus;

import java.time.LocalDateTime;

public record BloodRequestResponse(
        Long id,
        BloodGroup bloodGroup,
        String hospitalName,
        String hospitalAddress,
        String patientName,
        String patientPhone,
        int units,
        String reason,
        boolean emergency,
        RequestStatus status,
        LocalDateTime createdAt,
        LocalDateTime acceptedAt,
        LocalDateTime completedAt,

        Long patientId,
        Double patientLatitude,
        Double patientLongitude,

        Long donorId,
        String donorName,
        String donorPhone,
        String donorPhoto,
        Double donorLatitude,
        Double donorLongitude
) {
    public static BloodRequestResponse from(BloodRequest request) {
        return new BloodRequestResponse(
                request.getId(),
                request.getBloodGroup(),
                request.getHospitalName(),
                request.getHospitalAddress(),
                request.getPatientName(),
                request.getPhone(),
                request.getUnits(),
                request.getReason(),
                request.isEmergency(),
                request.getStatus(),
                request.getCreatedAt(),
                request.getAcceptedAt(),
                request.getCompletedAt(),

                request.getPatient().getId(),
                request.getPatient().getLatitude(),
                request.getPatient().getLongitude(),

                request.getDonor() != null ? request.getDonor().getId() : null,
                request.getDonor() != null ? request.getDonor().getName() : null,
                request.getDonor() != null ? request.getDonor().getPhone() : null,
                request.getDonor() != null ? request.getDonor().getPhoto() : null,
                request.getDonor() != null ? request.getDonor().getLatitude() : null,
                request.getDonor() != null ? request.getDonor().getLongitude() : null
        );
    }
}
