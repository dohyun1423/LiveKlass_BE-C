package com.be_c.liveklass.notification.sender;

import com.be_c.liveklass.notification.domain.NotificationChannel;
import com.be_c.liveklass.notification.domain.NotificationRequest;

public interface NotificationSender {

    boolean supports(NotificationChannel channel);

    void send(NotificationRequest noti);
}