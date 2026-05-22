package com.be_c.liveklass.notification.repository;

import com.be_c.liveklass.notification.domain.NotificationChannel;
import com.be_c.liveklass.notification.domain.NotificationRequest;
import com.be_c.liveklass.notification.domain.NotificationStatus;
import com.be_c.liveklass.notification.domain.NotificationType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NotificationRequestRepository extends JpaRepository<NotificationRequest, Long> {

    // 동일 이벤트 대한 중복 알림 요청 확인
    Optional<NotificationRequest> findByRecipientIdAndTypeAndEventIdAndChannel(
            Long recipientId,
            NotificationType type,
            String eventId,
            NotificationChannel channel
    );

    // 특정 사용자 전체 알림 목록 조회
    List<NotificationRequest> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    // 특정 사용자 안 읽은 알림 목록 조회
    List<NotificationRequest> findByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(Long recipientId);

    // 특정 사용자 읽은 알림 목록 조회
    List<NotificationRequest> findByRecipientIdAndReadAtIsNotNullOrderByCreatedAtDesc(Long recipientId);

    // 발송 가능한 알림 오래된 순서로 조회, 동시에 다른 워커가 잡지 못하게 잠금
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<NotificationRequest> findTop10ByStatusInAndScheduledAtLessThanEqualAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
            Collection<NotificationStatus> statuses,
            LocalDateTime scheduledAt,
            LocalDateTime nextRetryAt
    );

    // PROCESSING 상태로 오래 멈춘 알림 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<NotificationRequest> findByStatusAndProcessingStartedAtLessThanEqual(
            NotificationStatus status,
            LocalDateTime processingStartedAt
    );
}