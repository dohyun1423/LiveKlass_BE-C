package com.be_c.liveklass.notification.worker;

import com.be_c.liveklass.notification.domain.NotificationRequest;
import com.be_c.liveklass.notification.domain.NotificationStatus;
import com.be_c.liveklass.notification.repository.NotificationRequestRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationProcessTxService {

    private static final int PROCESSING_TIMEOUT_MINUTES = 5;

    private final NotificationRequestRepository repo;
    private final RetryPolicy retryPolicy;

    @Transactional
    public void recoverStuck() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime timeout = now.minusMinutes(PROCESSING_TIMEOUT_MINUTES);

        List<NotificationRequest> stuckNotis =
                repo.findByStatusAndProcessingStartedAtLessThanEqual(NotificationStatus.PROCESSING, timeout);

        stuckNotis.forEach(noti -> noti.recover(now));
    }

    @Transactional
    public List<NotificationRequest> claimReady() {
        LocalDateTime now = LocalDateTime.now();

        List<NotificationRequest> notis =
                repo.findTop10ByStatusInAndScheduledAtLessThanEqualAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
                        List.of(NotificationStatus.PENDING, NotificationStatus.FAILED),
                        now,
                        now
                );

        notis.forEach(noti -> noti.startProcessing(now));

        return notis;
    }

    @Transactional
    public void markSent(Long id) {
        NotificationRequest noti = getNoti(id);
        noti.markSent(LocalDateTime.now());
    }

    @Transactional
    public void markFailed(Long id, String reason) {
        NotificationRequest noti = getNoti(id);
        LocalDateTime failedAt = LocalDateTime.now();
        LocalDateTime nextRetryAt = retryPolicy.nextRetryAt(noti.getRetryCount(), failedAt);

        noti.markSendFailed(reason, nextRetryAt, failedAt);
    }

    private NotificationRequest getNoti(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found. id=" + id));
    }
}