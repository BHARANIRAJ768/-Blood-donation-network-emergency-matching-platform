package com.lifelink.dto;

import jakarta.validation.constraints.*;

public record BloodRequestCreateRequest(
        @NotBlank(message = "Blood group is required")
        String bloodGroup,

        @NotBlank(message = "Hospital name is required")
        @Size(max = 200, message = "Hospital name must be at most 200 characters")
        String hospitalName,

        @Size(max = 300, message = "Hospital address must be at most 300 characters")
        String hospitalAddress,

        @NotBlank(message = "Patient name is required")
        @Size(max = 120, message = "Patient name must be at most 120 characters")
        String patientName,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be exactly 10 digits")
        String phone,

        @NotNull(message = "Required units is required")
        @Min(value = 1, message = "Units must be at least 1")
        @Max(value = 10, message = "Units cannot exceed 10")
        Integer units,

        @Size(max = 500, message = "Reason must be at most 500 characters")
        String reason,

        boolean emergency,

        Long donorId,

        Integer patientAge,

        String requiredDate,

        Double latitude,

        Double longitude
) {
}
