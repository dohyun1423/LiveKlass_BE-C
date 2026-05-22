package com.be_c.liveklass.notification.domain;

// 알림의 상태 범위 지정
public enum NotificationStatus {
    PENDING,    // 접수
    PROCESSING, // 처리중
    SENT,       // 발송 성공
    FAILED,     // 실패 (재시도 가능)
    DEAD,       // 최종 실패 (재시도 불가능)
    CANCELED    // 알림 취소
}