package com.be_c.liveklass.notification.dto;

import com.be_c.liveklass.notification.domain.NotificationChannel;
import com.be_c.liveklass.notification.domain.NotificationRequest;
import com.be_c.liveklass.notification.domain.NotificationStatus;
import com.be_c.liveklass.notification.domain.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        Long recipientId,
        NotificationType notificationType,
        String eventId,
        Long lectureId,
        NotificationChannel channel,
        NotificationStatus status,
        String title,
        String message,
        String referenceData,
        int retryCount,
        int maxRetryCount,
        String lastFailureReason,
        LocalDateTime scheduledAt,
        LocalDateTime nextRetryAt,
        LocalDateTime sentAt,
        LocalDateTime readAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static NotificationResponse from(NotificationRequest noti) {
        return new NotificationResponse(
                noti.getId(),
                noti.getRecipientId(),
                noti.getType(),
                noti.getEventId(),
                noti.getLectureId(),
                noti.getChannel(),
                noti.getStatus(),
                noti.getTitle(),
                noti.getMessage(),
                noti.getReferenceData(),
                noti.getRetryCount(),
                noti.getMaxRetryCount(),
                noti.getLastFailureReason(),
                noti.getScheduledAt(),
                noti.getNextRetryAt(),
                noti.getSentAt(),
                noti.getReadAt(),
                noti.getCreatedAt(),
                noti.getUpdatedAt()
        );
    }
}
