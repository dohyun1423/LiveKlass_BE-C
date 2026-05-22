package com.be_c.liveklass.notification.worker;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class RetryPolicy {

    public LocalDateTime nextRetryAt(int retryCount, LocalDateTime now) {
        return switch (retryCount) {
            case 0 -> now.plusMinutes(1);
            case 1 -> now.plusMinutes(5);
            default -> now.plusMinutes(15);
        };
    }
}