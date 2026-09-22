package com.lifelink.util;

import com.lifelink.enums.BloodGroup;
import com.lifelink.enums.RequestStatus;
import com.lifelink.enums.Role;
import com.lifelink.exception.BadRequestException;

/**
 * Converts optional REST query parameters into strongly typed values.
 */
public final class QueryParsers {

    private QueryParsers() {
    }

    public static BloodGroup bloodGroup(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return BloodGroup.from(raw);
    }

    public static RequestStatus status(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return RequestStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid status value: " + raw);
        }
    }

    public static Role role(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Role.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid role value: " + raw);
        }
    }

    public static int page(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(raw.trim()));
        } catch (NumberFormatException ex) {
            throw new BadRequestException("Invalid page value: " + raw);
        }
    }

    public static int size(String raw, int defaultSize, int maxSize) {
        if (raw == null || raw.isBlank()) {
            return defaultSize;
        }
        try {
            int size = Integer.parseInt(raw.trim());
            if (size < 1) {
                return defaultSize;
            }
            return Math.min(size, maxSize);
        } catch (NumberFormatException ex) {
            throw new BadRequestException("Invalid size value: " + raw);
        }
    }
}
