package com.be_c.liveklass.notification.service;

import com.be_c.liveklass.notification.domain.NotificationChannel;
import com.be_c.liveklass.notification.domain.NotificationStatus;
import com.be_c.liveklass.notification.domain.NotificationType;
import com.be_c.liveklass.notification.dto.NotificationCreateRequest;
import com.be_c.liveklass.notification.dto.NotificationResponse;
import com.be_c.liveklass.notification.repository.NotificationRequestRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class NotificationServiceTest {

    @Autowired
    NotificationService service;

    @Autowired
    NotificationRequestRepository repo;

    @AfterEach
    void tearDown() {
        repo.deleteAll();
    }

    @Test
    void 알림_생성시_PENDING_상태로_저장된다() {
        NotificationResponse res = service.create(createReq("event-1", 1L));

        assertThat(res.id()).isNotNull();
        assertThat(res.status()).isEqualTo(NotificationStatus.PENDING);
        assertThat(res.retryCount()).isZero();
    }

    @Test
    void 동일_이벤트는_중복_생성되지_않는다() {
        service.create(createReq("event-dup", 1L));
        service.create(createReq("event-dup", 1L));

        List<NotificationResponse> list = service.getByUser(1L, null);

        assertThat(list).hasSize(1);
    }

    @Test
    void 사용자_알림_목록을_조회한다() {
        service.create(createReq("event-list-1", 1L));
        service.create(createReq("event-list-2", 1L));
        service.create(createReq("event-list-3", 2L));

        List<NotificationResponse> user1List = service.getByUser(1L, null);

        assertThat(user1List).hasSize(2);
        assertThat(user1List)
                .allMatch(noti -> noti.recipientId().equals(1L));
    }

    @Test
    void 다른_사용자의_알림은_읽음_처리할_수_없다() {
        NotificationResponse res = service.create(createReq("event-owner", 1L));

        assertThatThrownBy(() -> service.markRead(res.id(), 999L))
                .isInstanceOf(ResponseStatusException.class);
    }

    private NotificationCreateRequest createReq(String eventId, Long recipientId) {
        return new NotificationCreateRequest(
                recipientId,
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