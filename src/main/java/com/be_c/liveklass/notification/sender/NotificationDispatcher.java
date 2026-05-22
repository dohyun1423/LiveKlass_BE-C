package com.be_c.liveklass.notification.sender;

import com.be_c.liveklass.notification.domain.NotificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationDispatcher {

    private final List<NotificationSender> senders;

    public void send(NotificationRequest noti) {
        if (noti.getEventId().contains("fail")) {
            throw new RuntimeException("Mock send failure");
        }

        NotificationSender sender = senders.stream()
                .filter(it -> it.supports(noti.getChannel()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported channel: " + noti.getChannel()));

        sender.send(noti);
    }
}