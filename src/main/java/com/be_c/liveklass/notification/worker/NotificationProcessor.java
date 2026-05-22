package com.be_c.liveklass.notification.worker;

import com.be_c.liveklass.notification.domain.NotificationRequest;
import com.be_c.liveklass.notification.domain.NotificationStatus;
import com.be_c.liveklass.notification.repository.NotificationRequestRepository;
import com.be_c.liveklass.notification.sender.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationProcessor {

    private static final int PROCESSING_TIMEOUT_MINUTES = 10;

    private final NotificationRequestRepository repo;
    private final NotificationDispatcher dispatcher;
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
    public void processReady() {
        LocalDateTime now = LocalDateTime.now();

        List<NotificationRequest> notis =
                repo.findTop10ByStatusInAndScheduledAtLessThanEqualAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
                        List.of(NotificationStatus.PENDING, NotificationStatus.FAILED),
                        now,
                        now
                );

        for (NotificationRequest noti : notis) {
            process(noti, now);
        }
    }

    private void process(NotificationRequest noti, LocalDateTime now) {
        noti.startProcessing(now);

        try {
            dispatcher.send(noti);
            noti.markSent(LocalDateTime.now());
        } catch (Exception e) {
            String reason = e.getMessage();

            if (noti.canRetry()) {
                LocalDateTime nextRetryAt = retryPolicy.nextRetryAt(noti.getRetryCount(), LocalDateTime.now());
                noti.markFailed(reason, nextRetryAt, LocalDateTime.now());
                return;
            }

            noti.markDead(reason, LocalDateTime.now());
        }
    }
}