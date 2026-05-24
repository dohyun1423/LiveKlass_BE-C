package com.be_c.liveklass.notification.worker;

import com.be_c.liveklass.notification.domain.NotificationChannel;
import com.be_c.liveklass.notification.domain.NotificationStatus;
import com.be_c.liveklass.notification.domain.NotificationType;
import com.be_c.liveklass.notification.dto.NotificationCreateRequest;
import com.be_c.liveklass.notification.dto.NotificationResponse;
import com.be_c.liveklass.notification.repository.NotificationRequestRepository;
import com.be_c.liveklass.notification.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class NotificationProcessorTest {

    @Autowired
    NotificationService service;

    @Autowired
    NotificationProcessor processor;

    @Autowired
    NotificationRequestRepository repo;

    @AfterEach
    void tearDown() {
        repo.deleteAll();
    }

    @Test
    void 발송_성공시_SENT_상태가_된다() {
        NotificationResponse created = service.create(createReq("event-success"));

        processor.processReady();

        NotificationResponse result = service.get(created.id());

        assertThat(result.status()).isEqualTo(NotificationStatus.SENT);
        assertThat(result.sentAt()).isNotNull();
    }

    @Test
    void 발송_실패시_FAILED_상태와_실패사유가_저장된다() {
        NotificationResponse created = service.create(createReq("event-fail-1"));

        processor.processReady();

        NotificationResponse result = service.get(created.id());

        assertThat(result.status()).isEqualTo(NotificationStatus.FAILED);
        assertThat(result.retryCount()).isEqualTo(1);
        assertThat(result.lastFailureReason()).contains("Mock send failure");
        assertThat(result.nextRetryAt()).isNotNull();
    }

    @Test
    void 최대_실패_횟수에_도달하면_DEAD_상태가_된다() {
        NotificationResponse created = service.create(createReq("event-fail-dead"));

        processor.processReady();

        repo.findById(created.id()).ifPresent(noti -> {
            noti.markSendFailed("Mock send failure", LocalDateTime.now().minusSeconds(1), LocalDateTime.now());
            noti.markSendFailed("Mock send failure", LocalDateTime.now().minusSeconds(1), LocalDateTime.now());
            repo.save(noti);
        });

        NotificationResponse result = service.get(created.id());

        assertThat(result.status()).isEqualTo(NotificationStatus.DEAD);
        assertThat(result.retryCount()).isEqualTo(3);
    }

    private NotificationCreateRequest createReq(String eventId) {
        return new NotificationCreateRequest(
                1L,
                NotificationType.COURSE_ENROLLMENT_COMPLETED,
                eventId,
                10L,
                NotificationChannel.IN_APP,
                "title",
                "message",
                "{\"eventId\":\"" + eventId + "\"}",
                LocalDateTime.now()
        );
    }
}