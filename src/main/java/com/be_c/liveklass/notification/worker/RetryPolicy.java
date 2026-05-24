package com.be_c.liveklass.notification.worker;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

// worker의 재시도 정책
@Component
public class RetryPolicy {

    public LocalDateTime nextRetryAt(int retryCount, LocalDateTime now) {
        return switch (retryCount) {
            case 0 -> now.plusSeconds(10);
            case 1 -> now.plusSeconds(20);
            default -> now.plusSeconds(30);
        };
    }
}