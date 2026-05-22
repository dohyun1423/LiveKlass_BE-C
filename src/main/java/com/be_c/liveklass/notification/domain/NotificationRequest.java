package com.be_c.liveklass.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "notification_requests",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_notification_event_channel",
                        columnNames = {"recipient_id", "notification_type", "event_id", "channel"}
                )
        },
        indexes = {
                @Index(name = "idx_notification_recipient", columnList = "recipient_id"),
                @Index(name = "idx_notification_status_retry", columnList = "status, next_retry_at"),
                @Index(name = "idx_notification_scheduled", columnList = "scheduled_at")
        }
)
public class NotificationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 80)
    private NotificationType type;

    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    @Column(name = "lecture_id")
    private Long lectureId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private NotificationStatus status;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "message", length = 1000)
    private String message;

    @Column(name = "reference_data", columnDefinition = "TEXT")
    private String referenceData;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "max_retry_count", nullable = false)
    private int maxRetryCount;

    @Column(name = "last_failure_reason", columnDefinition = "TEXT")
    private String lastFailureReason;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public NotificationRequest(
            Long recipientId,
            NotificationType type,
            String eventId,
            Long lectureId,
            NotificationChannel channel,
            String title,
            String message,
            String referenceData,
            LocalDateTime scheduledAt
    ) {
        this.recipientId = recipientId;
        this.type = type;
        this.eventId = eventId;
        this.lectureId = lectureId;
        this.channel = channel;
        this.title = title;
        this.message = message;
        this.referenceData = referenceData;
        this.status = NotificationStatus.PENDING;
        this.retryCount = 0;
        this.maxRetryCount = 3;
        this.scheduledAt = scheduledAt;
        this.nextRetryAt = scheduledAt;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public boolean isUnread() {
        return readAt == null;
    }

    public void markRead() {
        if (readAt == null) {
            readAt = LocalDateTime.now();
            updatedAt = LocalDateTime.now();
        }
    }

    public void startProcessing(LocalDateTime now) {
        this.status = NotificationStatus.PROCESSING;
        this.processingStartedAt = now;
        this.updatedAt = now;
    }

    public void markSent(LocalDateTime now) {
        this.status = NotificationStatus.SENT;
        this.sentAt = now;
        this.lastFailureReason = null;
        this.updatedAt = now;
    }

    public void markSendFailed(String reason, LocalDateTime nextRetryAt, LocalDateTime now) {
        this.retryCount++;
        this.lastFailureReason = reason;
        this.processingStartedAt = null;

        if (retryCount >= maxRetryCount) {
            this.status = NotificationStatus.DEAD;
            this.nextRetryAt = null;
        } else {
            this.status = NotificationStatus.FAILED;
            this.nextRetryAt = nextRetryAt;
        }

        this.updatedAt = now;
    }

    public void recover(LocalDateTime now) {
        this.status = NotificationStatus.PENDING;
        this.processingStartedAt = null;
        this.updatedAt = now;
    }

}
