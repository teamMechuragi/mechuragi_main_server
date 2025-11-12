# 투표 종료 및 종료 10분 전 실시간 알림 시스템

## 📋 목차
1. [개요](#개요)
2. [시스템 아키텍처](#시스템-아키텍처)
3. [구현된 주요 기능](#구현된-주요-기능)
4. [알림 흐름 상세](#알림-흐름-상세)
5. [주요 컴포넌트 설명](#주요-컴포넌트-설명)
6. [메시지 포맷 및 채널 정의](#메시지-포맷-및-채널-정의)
7. [주의사항 및 베스트 프랙티스](#주의사항-및-베스트-프랙티스)

---

## 개요

### 목적
투표 작성자에게 **투표 종료 및 10분 전 알림**을 실시간으로 전송하는 시스템입니다. 알림 설정이 ON인 사용자에게만 알림을 보내며, 알림 내역을 MySQL에 영구 저장합니다.

### 세부 기능
- **개인화된 알림**: 투표 작성자에게만 알림 전송
- **알림 설정 지원**: 사용자별 알림 ON/OFF 기능
- **영구 저장**: 알림 내역을 DB에 저장하여 조회 및 읽음 처리 가능
- **중복 방지**: DB 기반 중복 발송 방지
- **분산 환경 지원**: Redis Pub/Sub을 통해 여러 서버 인스턴스 간 이벤트 공유
- **낮은 지연시간**: Redis Pub/Sub + WebSocket STOMP를 통한 실시간 전송

### 알림 종류
1. **투표 종료 10분 전 알림** (`ENDING_SOON`): 투표 마감 10분 전 작성자에게 알림
2. **투표 종료 알림** (`COMPLETED`): 투표 종료 시 작성자에게 알림

---

## 시스템 아키텍처

```
┌──────────────────┐     ┌──────────────────┐     ┌──────────────────┐
│  VoteNotification│────▶│   VotePost       │────▶│  NotificationS   │
│    Scheduler     │     │    Service       │     │    ervice        │
│  (매 1분 실행)    │     │  (Publisher)     │     │  (알림 저장)      │
└──────────────────┘     └──────────────────┘     └──────────────────┘
                                   │                         │
                                   │                         ▼
                                   │                  ┌──────────────┐
                                   │                  │    MySQL     │
                                   │                  │ (Notification│
                                   │                  │   Entity)    │
                                   ▼                  └──────────────┘
                         ┌──────────────────┐
                         │  redisPubSubTemp │
                         │     late         │
                         │  (RedisCacheConf)│
                         └──────────────────┘
                                   │
                                   ▼
                         ┌──────────────────┐
                         │ Redis Pub/Sub    │
                         │ vote:end         │
                         │ vote:before10min │
                         └──────────────────┘
                                   │
                                   ▼
                         ┌──────────────────┐
                         │VoteNotification  │
                         │   Subscriber     │
                         │ (MessageListener)│
                         └──────────────────┘
                                   │
                                   ▼
                         ┌──────────────────┐
                         │VoteNotification  │
                         │    Service       │
                         │ (STOMP 전송)      │
                         └──────────────────┘
                                   │
                                   ▼
                         ┌──────────────────┐
                         │  STOMP WebSocket │
                         │ /user/{memberId} │
                         │ /queue/vote/end  │
                         └──────────────────┘
                                   │
                                   ▼
                         ┌──────────────────┐
                         │     Client       │
                         │   (Frontend)     │
                         └──────────────────┘
```

---

## 구현된 주요 기능

### 1. 알림 설정 관리
- **Member 엔티티**: `voteNotificationEnabled` 필드 추가 (기본값: `true`)
- **설정 메서드**: `Member.updateVoteNotificationSetting(Boolean enabled)`
- 알림 설정이 OFF면 알림을 발송하지 않음

### 2. 알림 영구 저장
- **Notification 엔티티**: 알림 데이터를 MySQL에 저장
  - `member`: 알림을 받을 사용자
  - `voteId`: 투표 ID
  - `title`: 투표 제목
  - `type`: 알림 타입 (COMPLETED / ENDING_SOON)
  - `isRead`: 읽음 여부
  - `createdAt`: 생성 시간
  - `readAt`: 읽은 시간

### 3. 알림 CRUD API
- **NotificationController**: 알림 관련 REST API
  - `GET /api/notifications`: 알림 목록 조회 (페이징)
  - `PATCH /api/notifications/{id}/read`: 특정 알림 읽음 처리
  - `GET /api/notifications/unread-count`: 안 읽은 알림 개수 조회

### 4. 중복 알림 방지
- **NotificationRepository**: `existsByMemberIdAndVoteIdAndType()` 메서드로 중복 체크
- 동일 사용자에게 동일 투표의 동일 타입 알림은 한 번만 저장

### 5. 특정 사용자 타겟팅
- **VoteNotificationService**: `sendNotificationToUser(memberId, message)` 메서드
- STOMP의 `/user/{memberId}/queue/vote/end` 구조로 특정 사용자에게만 전송

---

## 알림 흐름 상세

### A. 투표 종료 10분 전 알림 흐름

```
1. VoteNotificationScheduler.notifyVotesEndingSoon()
   - 매 1분마다 실행 (@Scheduled(cron = "0 * * * * *"))
   - VotePostRepository.findVotesEndingInTenMinutes() 호출
   - 10분 후 ~ 11분 후 사이에 종료되는 투표 검색
   - notified10MinBefore = false인 투표만 조회
   ↓

2. VotePostService.notifyVoteEndingSoon(voteId, title)
   - VotePost 조회 및 author 확인
   - author.voteNotificationEnabled 체크 → OFF면 중단
   - NotificationService.createNotification() 호출 (알림 DB 저장)
   - VoteNotificationMessageDTO 생성 (memberId 포함)
   - redisPubSubTemplate.convertAndSend("vote:before10min", message) 호출
   - votePost.markNotified10MinBefore() 플래그 설정
   ↓

3. RedisCacheConfig.redisPubSubTemplate (Publisher 역할)
   - RedisTemplate<String, Object> Bean
   - GenericJackson2JsonRedisSerializer로 메시지 직렬화
   - Redis Pub/Sub 채널 "vote:before10min"로 메시지 발행
   ↓

4. VoteNotificationSubscriber.onMessage(message, pattern)
   - RedisMessageListenerContainer가 "vote:before10min" 채널 구독
   - MessageListener 인터페이스 구현
   - ObjectMapper로 메시지 역직렬화
   - VoteNotificationMessageDTO 추출 (memberId 포함)
   - message.memberId 존재 여부 확인
   ↓

5. VoteNotificationService.sendNotificationToUser(memberId, message)
   - SimpMessagingTemplate.convertAndSendToUser() 호출
   - destination: "/queue/vote/soon"
   - 특정 memberId에게만 STOMP 메시지 전송
   - 메트릭 기록 (VoteNotificationMetrics)
   ↓

6. STOMP WebSocket
   - 클라이언트가 구독: /user/queue/vote/soon
   - Spring WebSocket이 자동으로 /user/{memberId}/queue/vote/soon 라우팅
   - 해당 사용자에게만 실시간 메시지 전달
```

### B. 투표 종료 알림 흐름

```
1. VoteNotificationScheduler.completeExpiredVotes()
   - 매 1분마다 실행 (@Scheduled(cron = "0 * * * * *"))
   - VotePostRepository.findExpiredActiveVotes(now) 호출
   - 현재 시간보다 deadline이 지난 ACTIVE 투표 검색
   ↓

2. VotePostService.completeVoteAndNotify(voteId)
   - VotePost 조회
   - votePost.complete() 호출 (status → COMPLETED)
   - votePostRepository.save(votePost)
   - ApplicationEventPublisher.publishEvent(VoteCompletedEvent) 발행
   - VoteCompletedEvent: voteId, title, authorId 포함
   ↓

3. VoteEventListener.handleVoteCompleted(event)
   - @TransactionalEventListener(phase = AFTER_COMMIT)
   - 트랜잭션 커밋 후 실행 (DB 반영 확인 후 알림 발송)
   - MemberRepository.findById(authorId) 조회
   - author.voteNotificationEnabled 체크 → OFF면 중단
   - NotificationService.createNotification() 호출 (알림 DB 저장)
   - VoteNotificationMessageDTO 생성 (memberId 포함)
   - redisPubSubTemplate.convertAndSend("vote:end", message) 호출
   ↓

4. RedisCacheConfig.redisPubSubTemplate (Publisher 역할)
   - Redis Pub/Sub 채널 "vote:end"로 메시지 발행
   ↓

5. VoteNotificationSubscriber.onMessage(message, pattern)
   - "vote:end" 채널 구독 중
   - 메시지 역직렬화 및 memberId 추출
   ↓

6. VoteNotificationService.sendNotificationToUser(memberId, message)
   - destination: "/queue/vote/end"
   - 특정 memberId에게만 STOMP 메시지 전송
   ↓

7. STOMP WebSocket
   - 클라이언트가 구독: /user/queue/vote/end
   - 해당 사용자에게만 실시간 메시지 전달
```

---

## 주요 컴포넌트 설명

### 1. VoteNotificationScheduler
- **위치**: `global/scheduler/VoteNotificationScheduler.java`
- **역할**: 투표 종료 및 10분 전 알림 스케줄링
- **주요 메서드**:
  - `notifyVotesEndingSoon()`: 10분 전 알림 발송
  - `completeExpiredVotes()`: 만료된 투표 종료 처리
- **실행 주기**: 매 1분 (cron = "0 * * * * *")

### 2. VotePostService
- **위치**: `domain/vote/service/VotePostService.java`
- **역할**: 투표 비즈니스 로직 및 알림 발행
- **주요 메서드**:
  - `completeVoteAndNotify(voteId)`: 투표 종료 처리 및 이벤트 발행
  - `notifyVoteEndingSoon(voteId, title)`: 10분 전 알림 발행
- **의존성**:
  - `redisPubSubTemplate`: Redis Pub/Sub 메시지 발행
  - `notificationService`: 알림 DB 저장

### 3. NotificationService
- **위치**: `domain/notification/service/NotificationService.java`
- **역할**: 알림 CRUD 처리
- **주요 메서드**:
  - `createNotification()`: 알림 저장 (중복 체크 포함)
  - `getNotifications()`: 알림 목록 조회 (페이징)
  - `markAsRead()`: 특정 알림 읽음 처리
  - `getUnreadCount()`: 안 읽은 알림 개수 조회

### 4. VoteEventListener
- **위치**: `domain/notification/event/VoteEventListener.java`
- **역할**: VoteCompletedEvent 처리 및 Redis 발행
- **특징**:
  - `@TransactionalEventListener(phase = AFTER_COMMIT)`
  - 트랜잭션 커밋 후 실행 (DB 반영 확인)
  - 알림 설정 확인 후 발송

### 5. RedisCacheConfig
- **위치**: `global/redis/publisher/RedisCacheConfig.java`
- **역할**: Redis 설정 및 redisPubSubTemplate Bean 제공
- **redisPubSubTemplate**:
  - RedisTemplate<String, Object> 타입
  - GenericJackson2JsonRedisSerializer 사용
  - Pub/Sub 메시지 직렬화/역직렬화

### 6. RedisSubscriberConfig
- **위치**: `global/redis/subscriber/RedisSubscriberConfig.java`
- **역할**: Redis 채널 구독 설정
- **구독 채널**:
  - `vote:end`: 투표 종료 알림
  - `vote:before10min`: 투표 10분 전 알림

### 7. VoteNotificationSubscriber
- **위치**: `global/redis/subscriber/VoteNotificationSubscriber.java`
- **역할**: Redis 메시지 수신 및 처리
- **구현**: MessageListener 인터페이스
- **처리 흐름**:
  1. Redis 메시지 수신
  2. ObjectMapper로 역직렬화
  3. memberId 존재 여부 확인
  4. VoteNotificationService.sendNotificationToUser() 호출

### 8. VoteNotificationService
- **위치**: `domain/notification/service/VoteNotificationService.java`
- **역할**: STOMP를 통한 실시간 알림 전송
- **주요 메서드**:
  - `sendNotificationToUser(memberId, message)`: 특정 사용자에게 STOMP 전송
  - `sendNotification(message)`: 브로드캐스트 (하위 호환성, @Deprecated)
- **의존성**:
  - `SimpMessagingTemplate`: STOMP 메시지 전송
  - `VoteNotificationMetrics`: 메트릭 기록

### 9. NotificationController
- **위치**: `domain/notification/controller/NotificationController.java`
- **역할**: 알림 REST API 제공
- **엔드포인트**:
  - `GET /api/notifications`: 알림 목록 조회
  - `PATCH /api/notifications/{id}/read`: 알림 읽음 처리
  - `GET /api/notifications/unread-count`: 안 읽은 알림 개수

### 10. Notification 엔티티
- **위치**: `domain/notification/entity/Notification.java`
- **주요 필드**:
  - `member`: 알림을 받을 사용자 (ManyToOne)
  - `voteId`: 투표 ID
  - `title`: 투표 제목
  - `type`: VoteNotificationType (COMPLETED / ENDING_SOON)
  - `isRead`: 읽음 여부
  - `createdAt`: 생성 시간
  - `readAt`: 읽은 시간
- **메서드**: `markAsRead()` - 읽음 처리

### 11. Member 엔티티 추가 필드
- **위치**: `domain/member/entity/Member.java`
- **추가 필드**: `voteNotificationEnabled` (Boolean, 기본값: true)
- **메서드**: `updateVoteNotificationSetting(Boolean enabled)`

---

## 메시지 포맷 및 채널 정의

### Redis 채널 네이밍

| 채널 이름 | 용도 | STOMP 엔드포인트 |
|----------|------|-----------------|
| `vote:end` | 투표 종료 알림 | `/user/queue/vote/end` |
| `vote:before10min` | 투표 10분 전 알림 | `/user/queue/vote/soon` |

### VoteNotificationMessageDTO 구조

```json
{
  "voteId": 123,
  "title": "점심 메뉴 투표",
  "type": "COMPLETED",
  "timestamp": "2025-11-12T14:30:00",
  "memberId": 456
}
```

### 클라이언트 STOMP 구독 방법

프론트엔드에서 다음과 같이 구독:
- **투표 종료 알림**: `/user/queue/vote/end`
- **투표 10분 전 알림**: `/user/queue/vote/soon`

Spring WebSocket이 자동으로 `/user/{memberId}/queue/vote/end` 형태로 라우팅합니다.

---

## 주의사항 및 베스트 프랙티스

### 1. 트랜잭션 이후 메시지 발행
- `@TransactionalEventListener(phase = AFTER_COMMIT)` 사용
- DB 반영 실패 시 잘못된 알림 방지
- 트랜잭션 롤백 시 Redis 메시지도 발행되지 않음

### 2. 알림 설정 확인 필수
- 알림 발송 전 반드시 `member.voteNotificationEnabled` 체크
- 설정이 OFF면 DB 저장 및 Redis 발행 모두 중단

### 3. 중복 알림 방지
- DB 레벨: `NotificationRepository.existsByMemberIdAndVoteIdAndType()` 체크
- 투표 레벨: `VotePost.notified10MinBefore` 플래그 활용
- 스케줄러가 1분마다 실행되므로 중복 체크 필수

### 4. memberId 기반 타겟팅
- VoteNotificationMessageDTO에 `memberId` 필드 포함
- VoteNotificationSubscriber에서 memberId 존재 여부 확인
- `sendNotificationToUser(memberId, message)` 사용

### 5. Serializer 통일
- RedisTemplate과 MessageListener에서 동일한 `GenericJackson2JsonRedisSerializer` 사용
- ObjectMapper 설정 공유 (LocalDateTime 등 직렬화 일관성)

### 6. 스케줄러 효율화
- `deadline` 필드에 인덱스 추가 권장:
  ```sql
  CREATE INDEX idx_votepost_deadline_status ON vote_posts(deadline, status);
  ```
- JPQL 쿼리에서 인덱스 활용 가능하도록 쿼리 최적화

### 7. 에러 핸들링
- Redis 연결 실패 시 로그 기록 및 계속 진행
- STOMP 전송 실패 시 메트릭 기록
- 메시지 파싱 실패 시 예외 로그 남기고 다음 메시지 처리

### 8. 메트릭 수집
- VoteNotificationMetrics를 통한 알림 발송 성공/실패 추적
- Redis 발행/구독 지연 시간 측정
- STOMP 메시지 전송 지연 시간 측정

### 9. STOMP 엔드포인트 구조
- `/topic/*`: 브로드캐스트 (모든 구독자)
- `/queue/*`: 개인 전송 (특정 사용자)
- `/user/queue/*`: Spring이 자동으로 memberId 기반 라우팅

### 10. 하위 호환성 유지
- VoteNotificationService에 기존 `sendNotification()` 메서드 유지 (@Deprecated)
- memberId가 없는 메시지는 브로드캐스트 방식으로 전송

---

## 구현 체크리스트

### 필수 구현 항목
- [x] Member 엔티티에 `voteNotificationEnabled` 필드 추가
- [x] Notification 엔티티 생성
- [x] NotificationRepository 생성 (중복 체크 쿼리 포함)
- [x] NotificationService 생성 (CRUD)
- [x] NotificationController 생성 (API)
- [x] VoteCompletedEvent에 `authorId` 필드 추가
- [x] VoteEventListener에 알림 설정 확인 로직 추가
- [x] VoteNotificationMessageDTO에 `memberId` 필드 추가
- [x] VoteNotificationService에 `sendNotificationToUser()` 추가
- [x] VoteNotificationSubscriber에 memberId 기반 라우팅 추가
- [x] VotePostService.notifyVoteEndingSoon()에 알림 설정 확인 추가
- [x] VoteNotificationScheduler 예외 처리 강화

### DB 인덱스 추가
- [ ] `CREATE INDEX idx_votepost_deadline_status ON vote_posts(deadline, status);`
- [ ] `CREATE INDEX idx_notification_member_created ON notifications(member_id, created_at);`
- [ ] `CREATE INDEX idx_notification_member_read ON notifications(member_id, is_read);`

### 테스트
- [ ] NotificationService 단위 테스트
- [ ] VoteEventListener 단위 테스트
- [ ] 알림 설정 ON/OFF 시나리오 테스트
- [ ] 중복 알림 방지 테스트
- [ ] E2E 테스트 (실제 투표 생성 → 알림 수신)

### 모니터링
- [ ] VoteNotificationMetrics 대시보드 구성
- [ ] 알림 발송 성공률 모니터링
- [ ] Redis Pub/Sub 지연 시간 모니터링
- [ ] STOMP 연결 수 모니터링

---

## 참고 자료

### 프로젝트 내 주요 파일
- `domain/member/entity/Member.java` - 알림 설정 필드
- `domain/notification/entity/Notification.java` - 알림 엔티티
- `domain/notification/service/NotificationService.java` - 알림 CRUD
- `domain/notification/service/VoteNotificationService.java` - STOMP 전송
- `domain/notification/event/VoteEventListener.java` - 이벤트 처리
- `domain/vote/service/VotePostService.java` - 투표 로직 및 발행
- `global/scheduler/VoteNotificationScheduler.java` - 스케줄러
- `global/redis/publisher/RedisCacheConfig.java` - Redis 설정
- `global/redis/subscriber/VoteNotificationSubscriber.java` - 구독자

### 외부 문서
- [Spring WebSocket Documentation](https://docs.spring.io/spring-framework/reference/web/websocket.html)
- [Spring Data Redis - Pub/Sub](https://docs.spring.io/spring-data/redis/reference/redis/pubsub.html)
- [STOMP Protocol](https://stomp.github.io/)
