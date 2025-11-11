# 투표 알림 시스템 테스트 가이드

## 📋 목차
1. [테스트 개요](#테스트-개요)
2. [테스트 실행](#테스트-실행)
3. [테스트 종류별 가이드](#테스트-종류별-가이드)
4. [메트릭 모니터링](#메트릭-모니터링)
5. [트러블슈팅](#트러블슈팅)

---

## 테스트 개요

### 작성된 테스트 파일

```
src/test/java/
├── domain/vote/service/
│   └── VotePostServiceTest.java                 (단위 테스트)
├── global/
│   ├── redis/subscriber/
│   │   └── VoteNotificationSubscriberTest.java  (단위 테스트)
│   └── scheduler/
│       └── VoteNotificationSchedulerTest.java   (단위 테스트)
└── integration/
    └── RedisPubSubIntegrationTest.java          (통합 테스트)
```

### 테스트 커버리지

| 컴포넌트 | 테스트 파일 | 테스트 수 | 커버리지 |
|---------|-----------|---------|---------|
| VotePostService | VotePostServiceTest | 5개 | 투표 종료, 알림 발행 |
| VoteNotificationSubscriber | VoteNotificationSubscriberTest | 5개 | Redis 메시지 수신 |
| VoteNotificationScheduler | VoteNotificationSchedulerTest | 6개 | 스케줄러 로직 |
| Redis Pub/Sub | RedisPubSubIntegrationTest | 4개 | 전체 통합 |

---

## 테스트 실행

### 1. 전체 테스트 실행

```bash
# Gradle을 사용한 전체 테스트
./gradlew test

# 또는 특정 프로파일로 실행
./gradlew test --tests "*" -Dspring.profiles.active=test
```

### 2. 특정 테스트 클래스만 실행

```bash
# VotePostServiceTest만 실행
./gradlew test --tests "com.mechuragi.mechuragi_server.domain.vote.service.VotePostServiceTest"

# VoteNotificationSubscriberTest만 실행
./gradlew test --tests "com.mechuragi.mechuragi_server.global.redis.subscriber.VoteNotificationSubscriberTest"

# 통합 테스트만 실행
./gradlew test --tests "com.mechuragi.mechuragi_server.integration.*"
```

#### 결과 확인
```bash
Start-Process .\build\reports\tests\test\index.html
```

### 3. 특정 테스트 메서드만 실행

```bash
# 투표 종료 테스트만 실행
./gradlew test --tests "*.VotePostServiceTest.completeVoteAndNotify_Success"
```

---

## 테스트 종류별 가이드

### 1. 단위 테스트 (Unit Tests)

#### VotePostServiceTest

**테스트하는 기능:**
- 투표 종료 처리 및 이벤트 발행
- 투표 종료 10분 전 알림 발행
- 만료된 투표 일괄 처리

**실행:**
```bash
./gradlew test --tests "*VotePostServiceTest"
```

**주요 검증 사항:**
- ✅ 투표 상태가 COMPLETED로 변경되는지
- ✅ VoteCompletedEvent가 발행되는지
- ✅ Redis에 올바른 메시지가 발행되는지

#### VoteNotificationSubscriberTest

**테스트하는 기능:**
- Redis Pub/Sub 메시지 수신
- 잘못된 메시지 형식 처리
- 예외 상황 처리

**실행:**
```bash
./gradlew test --tests "*VoteNotificationSubscriberTest"
```

**주요 검증 사항:**
- ✅ Redis 메시지를 올바르게 파싱하는지
- ✅ VoteNotificationService를 호출하는지
- ✅ 예외 발생 시 서비스가 중단되지 않는지

#### VoteNotificationSchedulerTest

**테스트하는 기능:**
- 투표 종료 10분 전 스케줄러
- 만료된 투표 종료 스케줄러
- 대량 투표 처리

**실행:**
```bash
./gradlew test --tests "*VoteNotificationSchedulerTest"
```

**주요 검증 사항:**
- ✅ Repository에서 올바른 투표를 조회하는지
- ✅ Service의 알림 메서드가 호출되는지
- ✅ 예외 발생 시에도 나머지 투표를 처리하는지

### 2. 통합 테스트 (Integration Tests)

#### RedisPubSubIntegrationTest

**테스트하는 기능:**
- 실제 Redis를 사용한 Pub/Sub
- 메시지 발행 및 구독
- 여러 채널 동시 처리

**사전 준비:**
```bash
# Redis 서버가 실행 중이어야 합니다
# application-test.yml 확인:
# redis.host: localhost
# redis.port: 6370 (테스트용 포트)
```

**실행:**
```bash
./gradlew test --tests "*RedisPubSubIntegrationTest"
```

**주요 검증 사항:**
- ✅ Redis 메시지가 실제로 전파되는지
- ✅ Subscriber가 메시지를 수신하는지
- ✅ 여러 메시지를 동시에 처리할 수 있는지

---

## 메트릭 모니터링

1. MeterRegistry 필드 추가: 타입별/채널별 메트릭 생성에 필요
2. recordNotificationSent(String): 알림 타입별 메트릭 수집
3. recordNotificationFailed(String): 실패 타입별 메트릭 수집
4. recordRedisMessageReceived(String): Redis 채널별 메트릭 수집
5. recordStompMessageSent(String): STOMP 목적지별 메트릭 수집

### 메트릭 구조
```
vote.notification.sent (총합 + 타입별)
├─ type: total
├─ notification_type: COMPLETED
└─ notification_type: ENDING_SOON

vote.notification.failed (총합 + 타입별)
├─ type: total
├─ notification_type: COMPLETED
└─ notification_type: ENDING_SOON

vote.redis.message.received (총합 + 채널별)
├─ channel: vote:end
└─ channel: vote:before10min

vote.stomp.message.sent (총합 + 목적지별)
├─ destination: /topic/vote/end
└─ destination: /topic/vote/soon
```

### 1. 메트릭 확인

테스트 실행 후 Spring Boot Actuator를 통해 메트릭 확인:

```bash
# 애플리케이션 실행
./gradlew bootRun

# 메트릭 엔드포인트 확인
curl http://localhost:8080/actuator/metrics
```

### 2. 투표 알림 관련 메트릭

```bash
# 알림 발송 성공 횟수
curl http://localhost:8080/actuator/metrics/vote.notification.sent

# 알림 발송 실패 횟수
curl http://localhost:8080/actuator/metrics/vote.notification.failed

# Redis 메시지 수신 횟수
curl http://localhost:8080/actuator/metrics/vote.redis.message.received

# STOMP 메시지 발송 횟수
curl http://localhost:8080/actuator/metrics/vote.stomp.message.sent

# 알림 처리 시간
curl http://localhost:8080/actuator/metrics/vote.notification.duration

# Redis 발행 시간
curl http://localhost:8080/actuator/metrics/vote.redis.publish.duration
```

### 3. Redis 헬스 체크

```bash
# 헬스 체크 엔드포인트
curl http://localhost:8080/actuator/health

# 예상 결과
{
  "status": "UP",
  "components": {
    "redis": {
      "status": "UP",
      "details": {
        "version": "7.x.x"
      }
    }
  }
}
```

---

## 트러블슈팅

### 1. 통합 테스트 실패

**문제:** RedisPubSubIntegrationTest 실패

**원인:** Redis 서버가 실행되지 않음

**해결:**
```bash
# Redis 서버 시작 (Docker 사용)
docker run -d -p 6370:6379 redis:latest

# 또는 로컬 Redis 시작
redis-server --port 6370
```

### 2. 단위 테스트에서 NPE 발생

**문제:** NullPointerException in VotePostServiceTest

**원인:** Mock 객체 설정 누락

**해결:**
```java
// @Mock, @InjectMocks 어노테이션 확인
// @ExtendWith(MockitoExtension.class) 확인
```

### 3. 스케줄러 테스트 타임아웃

**문제:** VoteNotificationSchedulerTest 시간 초과

**원인:** 실제 스케줄러가 실행됨

**해결:**
```java
// 테스트에서는 @Scheduled 어노테이션이 무시됨
// 직접 메서드를 호출하여 테스트
scheduler.notifyVotesEndingSoon();
```

### 4. 메시지 직렬화 오류

**문제:** Jackson serialization error

**원인:** LocalDateTime 직렬화 모듈 누락

**해결:**
```java
// ObjectMapper에 JavaTimeModule 등록
objectMapper.registerModule(new JavaTimeModule());
```

### 5. 통합 테스트에서 메시지를 받지 못함

**문제:** verify timeout 발생

**원인:** Redis Subscriber가 등록되지 않음

**해결:**
```java
// RedisSubscriberConfig 확인
// RedisMessageListenerContainer가 제대로 설정되었는지 확인
```

---

## 테스트 베스트 프랙티스

### 1. Given-When-Then 패턴 사용

```java
@Test
void testExample() {
    // given: 테스트 준비
    VotePost votePost = createTestVotePost();
    when(repository.findById(1L)).thenReturn(Optional.of(votePost));

    // when: 테스트 실행
    service.completeVoteAndNotify(1L);

    // then: 검증
    assertEquals(VoteStatus.COMPLETED, votePost.getStatus());
    verify(eventPublisher).publishEvent(any());
}
```

### 2. 테스트 격리

```java
@BeforeEach
void setUp() {
    // 각 테스트마다 새로운 객체 생성
    testVotePost = createTestVotePost();
}
```

### 3. 의미 있는 테스트 이름

```java
// ❌ 나쁜 예
@Test
void test1() { ... }

// ✅ 좋은 예
@Test
@DisplayName("투표 종료 처리 및 이벤트 발행 성공")
void completeVoteAndNotify_Success() { ... }
```

### 4. 엣지 케이스 테스트

```java
// 정상 케이스 + 예외 케이스 모두 테스트
@Test
void completeVoteAndNotify_VoteNotFound() {
    // 존재하지 않는 투표 처리
}

@Test
void onMessage_InvalidMessageFormat() {
    // 잘못된 메시지 형식 처리
}
```

---

## 테스트 실행 체크리스트

- [ ] Redis 서버가 실행 중인지 확인
- [ ] application-test.yml 설정 확인
- [ ] 모든 단위 테스트 통과
- [ ] 통합 테스트 통과
- [ ] 테스트 커버리지 확인
- [ ] 메트릭이 올바르게 기록되는지 확인
- [ ] 로그 출력이 정상인지 확인

---

## 다음 단계

테스트가 모두 통과하면:

1. **Swagger로 E2E 테스트** (선택사항)
   - WebSocket 연결 확인
   - 실제 알림 수신 확인

2. **프론트엔드 연동 테스트**
   - SockJS + STOMP 클라이언트 연결
   - 실시간 알림 수신 확인

3. **부하 테스트**
   - 동시에 많은 투표 생성
   - 알림 시스템 성능 측정

4. **운영 환경 배포 준비**
   - 프로덕션 Redis 설정
   - 모니터링 대시보드 설정
   - 알림 채널 확장 (이메일, 푸시 등)
