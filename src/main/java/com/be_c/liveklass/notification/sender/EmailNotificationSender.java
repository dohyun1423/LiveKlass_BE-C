package com.be_c.liveklass.notification.sender;

import com.be_c.liveklass.notification.domain.NotificationChannel;
import com.be_c.liveklass.notification.domain.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailNotificationSender implements NotificationSender {

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.EMAIL;
    }

    @Override
    public void send(NotificationRequest noti) {
        log.info("[EMAIL MOCK] recipientId={}, title={}, message={}",
                noti.getRecipientId(),
                noti.getTitle(),
                noti.getMessage()
        );
    }
}