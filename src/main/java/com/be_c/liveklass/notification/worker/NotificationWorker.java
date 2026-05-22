package com.be_c.liveklass.notification.worker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationWorker {

    private final NotificationProcessor processor;

    @Scheduled(fixedDelay = 5000)
    public void run() {
        try {
            processor.recoverStuck();
            processor.processReady();
        } catch (Exception e) {
            log.error("Notification worker failed", e);
        }
    }
}