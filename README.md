# 프로덕트 엔지니어 채용 과제 BE-C 알림 발송 시스템

## 프로젝트 개요

수강 신청, 결제 확정, 강의 시작 D-1, 취소 처리와 같은 이벤트를 기반으로 사용자에게 이메일 또는 인앱 알림을 발송하는 백엔드 과제입니다.

알림 발송은 비즈니스 트랜잭션에 영향을 주지 않아야 하므로, API 요청 시점에는 알림 요청만 저장하고 실제 발송은 별도 Worker가 비동기로 처리하도록 구현했습니다. 실제 이메일 서버와 메시지 브로커는 사용하지 않고, Mock Sender와 DB 기반 큐 구조로 대체했습니다.

알림 처리 실패가 비즈니스 트랜잭션에 영향을 주어서는 안 됩니다. 단, 예외를 단순히 무시하는 방식으로 이를 달성해서는 안 됩니다.
네트워크 장애, 외부 이메일 서버 오류 등 일시적 장애에 대비해 재시도가 가능해야 합니다.
동일한 이벤트에 대해 알림이 중복 발송되면 안 됩니다.
## 기술 스택

- Java 21
- Spring Boot
- Spring Web MVC
- Spring Data JPA
- MySQL
- Gradle
- Lombok

선택 사항:

- ORM: JPA
- DB: MySQL
- 인증/인가: `X-USER-ID` 헤더 또는 userId 파라미터 기반 간략 처리

## 실행 방법

### 1. MySQL DB 생성

```sql
create database notification_system
character set utf8mb4
collate utf8mb4_unicode_ci;
```

### 2. DB 설정

`src/main/resources/application.properties`에서 로컬 MySQL 계정 정보를 확인합니다.

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/notification_system
spring.datasource.username=본인_DB_이름
spring.datasource.password=본인_DB_비밀번호

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

### 3. 애플리케이션 실행

Windows 기준:

```bash
.\gradlew.bat bootRun
```

실행 후 테스트 페이지:

```text
http://localhost:8080/Notification_test.html
```

## 요구사항 해석 및 가정
### 알림 처리 실패가 비즈니스 트랜잭션에 영향을 주지 않아야 한다

- 수강 신청, 결제 확정, 취소 처리 같은 핵심 비즈니스 로직과 알림 발송은 서로 다른 책임이라고 해석했습니다.

- 따라서 알림 발송 요청 API는 실제 이메일 또는 인앱 알림 발송을 직접 수행하지 않고, 알림 요청을 DB에 `PENDING` 상태로 저장한 뒤 즉시 응답합니다. 이후 실제 발송은 `NotificationWorker`가 별도 흐름에서 처리합니다.

- 이 구조에서는 외부 이메일 서버 장애나 네트워크 오류가 발생해도 원래의 비즈니스 트랜잭션은 실패하지 않습니다.

- 단, 예외를 단순히 무시하지 않기 위해 발송 실패 시 아래 정보를 DB에 기록합니다.
 
### 네트워크 장애, 외부 이메일 서버 오류 등 일시적 장애에 대비해 재시도가 가능해야 합니다.

- 외부 이메일 서버 오류나 네트워크 장애는 일시적으로 발생할 수 있다고 보고, 실패한 알림을 즉시 버리지 않고 재시도 가능한 상태로 보관합니다.
- 발송 실패 시 `FAILED` 상태로 변경하고 다음 재시도 가능 시각인 `nextRetryAt`을 저장합니다. Worker는 `nextRetryAt`이 지난 `FAILED` 알림을 다시 조회하여 발송을 재시도합니다.
- `RetryPolicy.java` 테스트 환경에서 재시도 횟수를 총 3회, 간격을 10초, 20초, 30초로 설정했으나 실제 운영환경에서 변경할 수 있습니다.
- 최대 실패 횟수에 도달한 알림은 `DEAD` 상태로 전환하여 자동 재시도 대상에서 제외합니다. 실패 사유는 `lastFailureReason`에 남겨 운영자가 원인을 확인할 수 있도록 했습니다.

### 동일한 이벤트에 대해 알림이 중복 발송되면 안 됩니다.

- 동일 이벤트의 중복 발송을 막기 위해 알림 생성 단계와 처리 단계에서 각각 방어했습니다.
- 생성 단계에서는 DB Unique Constraint를 사용했습니다.
  - recipient_id + notification_type + event_id + channel 
  - 이 조합이 같으면 같은 이벤트에 대한 같은 사용자, 같은 채널의 알림으로 판단합니다. 동시에 같은 요청이 여러 번 들어와도 DB 제약조건이 중복 생성을 막고, 서비스에서는 이미 존재하는 알림을 조회해 반환합니다.
- 처리 단계에서는 Worker가 처리 대상 알림을 조회할 때 pessimistic lock을 사용합니다.
  - @Lock(LockModeType.PESSIMISTIC_WRITE)
  - 그리고 처리 대상 알림을 먼저 PROCESSING 상태로 변경한 뒤 실제 발송을 수행합니다.
  - 다중 인스턴스 환경에서 동일 알림이 중복 처리되는 경우도 방어합니다.

### 처리 중 상태가 오래 지속되는 경우

- 알림 Worker가 알림을 처리하기 시작하면 상태를 `PROCESSING`으로 변경합니다.
    - PENDING 또는 FAILED -> PROCESSING

- 그런데 서버가 PROCESSING 상태로 변경한 직후 종료되거나, 발송 처리 중 예기치 못한 장애가 발생하면 해당 알림이 계속 PROCESSING 상태로 남을 수 있습니다.
- 이를 복구하기 위해 processingStartedAt 필드를 두었습니다. Worker는 주기적으로 PROCESSING 상태이면서 processingStartedAt이 기준 시간보다 오래된 알림을 조회합니다.
  - 현재 기준은 5분입니다.

- status = PROCESSING  processingStartedAt <= 현재 시각 - 5분
- 해당 조건에 맞는 알림은 다시 PENDING 상태로 복구합니다.

이렇게 하면 일시적인 서버 장애나 처리 중단으로 멈춘 알림도 다음 Worker 실행 시 다시 처리될 수 있습니다.




## 설계 결정과 이유

### 비동기 처리 구조

알림 요청과 실제 발송을 분리했습니다.

```text
POST /api/notifications
-> notification_requests 테이블에 PENDING 저장
-> API 응답
-> NotificationWorker가 주기적으로 조회
-> PROCESSING 선점
-> Mock Sender 발송
-> SENT / FAILED / DEAD
```

이 구조를 선택한 이유는 이메일 서버 장애나 네트워크 장애가 수강 신청, 결제 확정 같은 핵심 비즈니스 흐름을 실패시키지 않도록 하기 위해서입니다.

### 상태 정의

알림 상태는 다음과 같이 정의했습니다.

```text
PENDING    : 발송 대기
PROCESSING : Worker가 처리 중
SENT       : 발송 성공
FAILED     : 발송 실패, 재시도 가능
DEAD       : 최대 재시도 초과로 최종 실패
CANCELED   : 취소 확장용 상태
```

주요 상태 전이:

```text
PENDING -> PROCESSING -> SENT
PENDING -> PROCESSING -> FAILED
FAILED -> PROCESSING -> FAILED
FAILED -> PROCESSING -> DEAD
PROCESSING -> PENDING
```

`PROCESSING -> PENDING`은 처리 중 서버 장애 등으로 상태가 오래 멈춘 알림을 복구하기 위한 흐름입니다.

### PROCESSING 상태 커밋

초기 구조에서는 `PROCESSING -> SENT`가 하나의 트랜잭션 안에서 일어나 외부 조회 시 `PROCESSING`을 확인하기 어려웠습니다.

이를 해결하기 위해 `NotificationProcessTxService`에서 처리 대상 알림을 먼저 `PROCESSING` 상태로 선점하고 커밋한 뒤, 실제 발송과 최종 상태 변경을 별도 트랜잭션으로 처리했습니다.

```text
claimReady()
-> PROCESSING 저장 및 커밋
-> dispatcher.send()
-> markSent() 또는 markFailed()
```

### 재시도 정책

알림 발송 실패 시 실패 사유를 `lastFailureReason`에 기록하고 `retryCount`를 증가시킵니다.

현재 로컬 테스트 편의를 위해 재시도 간격은 짧게 설정했습니다.

```text
1회 실패 후 10초 뒤 재시도
2회 실패 후 20초 뒤 재시도
3회 실패 시 DEAD 처리
```

운영 환경에서는 더 긴 간격으로 조정하는 것을 가정했습니다.

### 중복 발송 방지

동일 이벤트에 대한 중복 알림 생성을 막기 위해 DB Unique Constraint를 사용했습니다.

중복 기준:

```text
recipient_id + notification_type + event_id + channel
```

동시에 같은 요청이 여러 번 들어와도 DB 제약조건이 최종적으로 중복 생성을 막습니다. 중복 저장 예외가 발생하면 기존 알림 요청을 조회하여 반환합니다.

### 다중 인스턴스 처리

Worker가 처리 대상 알림을 조회할 때 pessimistic lock을 사용합니다.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
```

이를 통해 여러 인스턴스가 동시에 같은 알림을 처리하는 상황을 방지하도록 설계했습니다.

### 읽음 처리

인앱 알림은 `readAt` 필드로 읽음 여부를 관리합니다.

```text
readAt == null : 안 읽음
readAt != null : 읽음
```
- 단, 실제 발송이 완료된 `SENT` 상태의 알림만 읽음 처리할 수 있습니다. 
- 여기서 `readAt`은 실제 이메일 오픈 추적이 아니라, 시스템에 저장된 알림 항목을 사용자가 확인했는지를 나타내는 값으로 해석했습니다.
여러 기기에서 동시에 읽음 처리 요청이 와도 이미 `readAt`이 있으면 다시 변경하지 않아 멱등적으로 처리됩니다.

## 미구현 / 제약사항

- 실제 이메일 발송은 구현하지 않고 Mock 로그 출력으로 대체했습니다.
- 실제 메시지 브로커는 사용하지 않았습니다.
- 알림 템플릿 관리는 구현하지 않았습니다.
- 최종 실패 알림에 대한 수동 재시도 API는 아직 구현하지 않았습니다.


- 테스트용 `eventId` 규칙인 `slow`, `fail`은 로컬 검증 편의를 위한 Mock 동작입니다.

## AI 활용 범위

AI 도구를 사용하여 다음 항목에 도움을 받았습니다.

- 알림 상태 전이 설계
- 비동기 처리 구조 설계
- JPA Entity, Repository, Service, Controller 코드 초안 작성
- 재시도 및 중복 방지 전략 검토
- README 구성 초안 작성

AI가 생성한 내용을 그대로 사용하지 않고, 요구사항에 맞게 직접 구조를 조정했습니다.

## API 목록 및 예시

### 1. 알림 발송 요청 등록

```http
POST /api/notifications
Content-Type: application/json
```

Request:

```json
{
  "recipientId": 1,
  "notificationType": "COURSE_ENROLLMENT_COMPLETED",
  "eventId": "enrollment-1001",
  "lectureId": 10,
  "channel": "IN_APP",
  "title": "수강 신청 완료",
  "message": "수강 신청이 완료되었습니다.",
  "referenceData": "{\"enrollmentId\":1001}"
}
```

Response:

```json
{
  "id": 1,
  "recipientId": 1,
  "notificationType": "COURSE_ENROLLMENT_COMPLETED",
  "eventId": "enrollment-1001",
  "lectureId": 10,
  "channel": "IN_APP",
  "status": "PENDING",
  "title": "수강 신청 완료",
  "message": "수강 신청이 완료되었습니다.",
  "referenceData": "{\"enrollmentId\":1001}",
  "retryCount": 0,
  "maxRetryCount": 3,
  "lastFailureReason": null,
  "scheduledAt": "2026-05-24T10:00:00",
  "nextRetryAt": "2026-05-24T10:00:00",
  "sentAt": null,
  "readAt": null,
  "createdAt": "2026-05-24T10:00:00",
  "updatedAt": "2026-05-24T10:00:00"
}
```

### 2. 예약 알림 등록

`scheduledAt`을 지정하면 해당 시각 이후 Worker가 처리합니다.

```json
{
  "recipientId": 1,
  "notificationType": "COURSE_START_D_MINUS_1",
  "eventId": "lecture-10-d-1",
  "lectureId": 10,
  "channel": "EMAIL",
  "title": "강의 시작 하루 전",
  "message": "내일 강의가 시작됩니다.",
  "referenceData": "{\"lectureId\":10}",
  "scheduledAt": "2026-05-24T18:00:00"
}
```

### 3. 알림 상태 조회

```http
GET /api/notifications/{id}
```

Example:

```http
GET /api/notifications/1
```

### 4. 사용자 알림 목록 조회

```http
GET /api/notifications/users/{userId}
```

Example:

```http
GET /api/notifications/users/1
```

읽음/안읽음 필터:

```http
GET /api/notifications/users/1?unreadOnly=true
GET /api/notifications/users/1?unreadOnly=false
```

### 5. 내 알림 목록 조회

```http
GET /api/notifications/me
X-USER-ID: 1
```

PowerShell 예시:

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8080/api/notifications/me" `
  -Headers @{ "X-USER-ID" = "1" }
```

### 6. 알림 읽음 처리

```http
PATCH /api/notifications/{id}/read
X-USER-ID: 1
```

PowerShell 예시:

```powershell
Invoke-RestMethod `
  -Method Patch `
  -Uri "http://localhost:8080/api/notifications/1/read" `
  -Headers @{ "X-USER-ID" = "1" }
```

## 데이터 모델 설명

### notification_requests

| 컬럼 | 설명 |
| --- | --- |
| id | 알림 요청 ID |
| recipient_id | 수신자 ID |
| notification_type | 알림 타입 |
| event_id | 이벤트 ID |
| lecture_id | 강의 ID |
| channel | 발송 채널, EMAIL / IN_APP |
| status | 알림 처리 상태 |
| title | 알림 제목 |
| message | 알림 내용 |
| reference_data | 참조 데이터 |
| retry_count | 현재 실패 횟수 |
| max_retry_count | 최대 실패 허용 횟수 |
| last_failure_reason | 마지막 실패 사유 |
| scheduled_at | 예약 발송 시각 |
| next_retry_at | 다음 재시도 가능 시각 |
| processing_started_at | 처리 시작 시각 |
| sent_at | 발송 성공 시각 |
| read_at | 읽음 처리 시각 |
| created_at | 생성 시각 |
| updated_at | 수정 시각 |

### 중복 방지 제약조건

```text
recipient_id + notification_type + event_id + channel
```

동일한 이벤트에 대해 같은 사용자에게 같은 채널로 알림이 중복 생성되지 않도록 합니다.


## 테스트 실행 방법

### 자동 테스트

아래 테스트를 실행하여 성공을 확인했습니다.

```bash
.\gradlew.bat test
```
실행 결과: 
BUILD SUCCESSFUL


개별 테스트도 성공을 확인했습니다.
```bash
.\gradlew.bat test --tests "com.be_c.liveklass.notification.service.NotificationServiceTest"
.\gradlew.bat test --tests "com.be_c.liveklass.notification.worker.NotificationProcessorTest"
```

NotificationServiceTest
```bash
- 알림 생성 시 PENDING 상태 저장 검증
- 동일 이벤트 중복 생성 방지 검증
- 사용자별 알림 목록 조회 검증
- 다른 사용자의 알림 읽음 처리 차단 검증
```

NotificationProcessorTest
```bash
- 발송 성공 시 SENT 상태 변경 검증
- 발송 실패 시 FAILED 상태 및 실패 사유 저장 검증
- 최대 실패 횟수 도달 시 DEAD 상태 변경 검증
```

테스트 리포트는 아래 경로에서 확인할 수 있습니다.
```bach
build/reports/tests/test/index.html
```

### 수동 테스트 페이지

```text
http://localhost:8080/Notification_test.html
```

수동 테스트 시나리오:

```text
1. 알림 등록
2. 등록 직후 PENDING 확인
3. Worker 처리 후 SENT 확인
4. 같은 eventId로 다시 등록하여 중복 생성 방지 확인
5. 사용자 알림 목록 조회
6. unreadOnly=true / false 필터 확인
7. 읽음 처리 후 readAt 변경 확인
8. fail 포함 eventId로 FAILED / DEAD 확인
9. slow 포함 eventId로 PROCESSING 확인
```

테스트용 eventId 규칙:

```text
일반 eventId:
정상 발송되어 SENT 처리

fail 포함:
Mock 발송 실패 발생, FAILED / DEAD 상태 확인

slow 포함:
Mock 발송 지연 발생, PROCESSING 상태 확인
```

예시:

```text
enrollment-1001
enrollment-fail-1001
enrollment-slow-1001
enrollment-slow-fail-1001
```
