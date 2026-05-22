package com.be_c.liveklass.notification.dto;

import com.be_c.liveklass.notification.domain.NotificationChannel;
import com.be_c.liveklass.notification.domain.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record NotificationCreateRequest(
        @NotNull Long recipientId,
        @NotNull NotificationType notificationType,
        @NotBlank String eventId,
        Long lectureId,
        @NotNull NotificationChannel channel,
        String title,
        String message,
        String referenceData,
        LocalDateTime scheduledAt
) {
}
