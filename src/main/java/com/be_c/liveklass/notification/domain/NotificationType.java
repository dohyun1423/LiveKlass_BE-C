package com.be_c.liveklass.notification.domain;

// 발생하는 이벤트 유형
public enum NotificationType {
    COURSE_ENROLLMENT_COMPLETED,    // 수강 신청 완료
    PAYMENT_CONFIRMED,              // 결제 확정
    COURSE_START_D_MINUS_1,         // 강의시작 D-1
    ENROLLMENT_CANCELED             // 취소 처리
}
