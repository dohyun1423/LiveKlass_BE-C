package com.be_c.liveklass.notification.worker;

import com.be_c.liveklass.notification.domain.NotificationRequest;
import com.be_c.liveklass.notification.sender.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationProcessor {

    private final NotificationProcessTxService txService;
    private final NotificationDispatcher dispatcher;

    public void recoverStuck() {
        txService.recoverStuck();
    }

    public void processReady() {
        List<NotificationRequest> notis = txService.claimReady();

        for (NotificationRequest noti : notis) {
            process(noti);
        }
    }

    private void process(NotificationRequest noti) {
        try {
            dispatcher.send(noti);
            txService.markSent(noti.getId());
        } catch (Exception e) {
            txService.markFailed(noti.getId(), e.getMessage());
        }
    }
}