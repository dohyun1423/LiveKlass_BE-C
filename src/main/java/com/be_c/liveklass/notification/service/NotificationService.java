package com.be_c.liveklass.notification.service;

import com.be_c.liveklass.notification.domain.NotificationRequest;
import com.be_c.liveklass.notification.dto.NotificationCreateRequest;
import com.be_c.liveklass.notification.dto.NotificationResponse;
import com.be_c.liveklass.notification.repository.NotificationRequestRepository;
import com.be_c.liveklass.notification.domain.NotificationStatus;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRequestRepository repo;

    @Transactional
    public NotificationResponse create(NotificationCreateRequest req) {
        LocalDateTime scheduleAt = req.scheduledAt() == null
                ? LocalDateTime.now()
                : req.scheduledAt();

        NotificationRequest noti = new NotificationRequest(
                req.recipientId(),
                req.notificationType(),
                req.eventId(),
                req.lectureId(),
                req.channel(),
                req.title(),
                req.message(),
                req.referenceData(),
                scheduleAt
        );

        try {
            return NotificationResponse.from(repo.save(noti));
        } catch (DataIntegrityViolationException e) {
            NotificationRequest duplicate = repo.findByRecipientIdAndTypeAndEventIdAndChannel(
                    req.recipientId(),
                    req.notificationType(),
                    req.eventId(),
                    req.channel()
            ).orElseThrow(() -> e);

            return NotificationResponse.from(duplicate);
        }
    }

    @Transactional(readOnly = true)
    public NotificationResponse get(Long id) {
        NotificationRequest noti = getNoti(id);
        return NotificationResponse.from(noti);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getByUser(Long userId, Boolean unreadOnly) {
        List<NotificationRequest> notis;

        if (Boolean.TRUE.equals(unreadOnly)) {
            notis = repo.findByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(userId);
        } else if (Boolean.FALSE.equals(unreadOnly)) {
            notis = repo.findByRecipientIdAndReadAtIsNotNullOrderByCreatedAtDesc(userId);
        } else {
            notis = repo.findByRecipientIdOrderByCreatedAtDesc(userId);
        }

        return notis.stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional
    public NotificationResponse markRead(Long id, Long userId) {
        NotificationRequest noti = getNoti(id);
        checkOwner(noti, userId);
        checkReadable(noti);

        noti.markRead();

        return NotificationResponse.from(noti);
    }

    private void checkReadable(NotificationRequest noti) {
        if (noti.getStatus() != NotificationStatus.SENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only sent notifications can be marked as read.");
        }
    }

    private NotificationRequest getNoti(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found. id=" + id));
    }

    private void checkOwner(NotificationRequest noti, Long userId) {
        if (!noti.getRecipientId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot access another user's notification.");
        }
    }
}