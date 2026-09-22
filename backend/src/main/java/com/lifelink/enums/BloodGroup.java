package com.lifelink.enums;

import com.lifelink.exception.BadRequestException;

public enum BloodGroup {

    A_POSITIVE("A+"),
    A_NEGATIVE("A-"),
    B_POSITIVE("B+"),
    B_NEGATIVE("B-"),
    AB_POSITIVE("AB+"),
    AB_NEGATIVE("AB-"),
    O_POSITIVE("O+"),
    O_NEGATIVE("O-");

    private final String value;

    BloodGroup(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static BloodGroup from(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("Blood group is required");
        }
        String normalized = raw.trim().toUpperCase();
        for (BloodGroup group : values()) {
            if (group.value.equals(normalized)) {
                return group;
            }
        }
        throw new BadRequestException("Invalid blood group: " + raw);
    }
}
