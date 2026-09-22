package com.lifelink.dto;

import com.lifelink.entity.Notification;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String title,
        String message,
        boolean read,
        Long requestId,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead(),
                notification.getRequestId(),
                notification.getCreatedAt()
        );
    }
}
