package com.lifelink.dto;

import com.lifelink.entity.BloodRequest;
import com.lifelink.enums.BloodGroup;
import com.lifelink.enums.RequestStatus;
import com.lifelink.enums.Role;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record HistoryResponse(
        Long id,
        BloodGroup bloodGroup,
        String patientName,
        String patientPhone,
        Long patientId,
        String donorName,
        String donorPhone,
        Long donorId,
        String hospitalName,
        int units,
        boolean emergency,
        RequestStatus status,
        LocalDate donationDate,
        LocalDateTime createdAt,
        LocalDateTime acceptedAt,
        LocalDateTime completedAt,
        Role myRole
) {
    public static HistoryResponse from(BloodRequest request, LocalDate donationDate, Role myRole) {
        return new HistoryResponse(
                request.getId(),
                request.getBloodGroup(),
                request.getPatientName(),
                request.getPhone(),
                request.getPatient().getId(),
                request.getDonor() != null ? request.getDonor().getName() : null,
                request.getDonor() != null ? request.getDonor().getPhone() : null,
                request.getDonor() != null ? request.getDonor().getId() : null,
                request.getHospitalName(),
                request.getUnits(),
                request.isEmergency(),
                request.getStatus(),
                donationDate,
                request.getCreatedAt(),
                request.getAcceptedAt(),
                request.getCompletedAt(),
                myRole
        );
    }
}
