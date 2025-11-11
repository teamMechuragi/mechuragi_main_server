# 투표 알림 시스템 테스트 트러블슈팅

## 1. VoteNotificationMetrics - MeterRegistry 등록 오류
**오류**: `register()` 메서드 호출 시 잘못된 파라미터 사용
```java
// 오류 코드
register(notificationSentCounter.getId().getTag("registry"))
```

**해결**: MeterRegistry 필드 추가 및 올바른 사용
```java
private final MeterRegistry registry;

Counter.builder("vote.notification.sent")
    .tag("notification_type", notificationType)
    .register(registry)  // registry 필드 사용
    .increment();
```

---

## 2. VoteNotificationSubscriberTest - 생성자 불일치
**오류**: Mock 객체 누락으로 인한 생성자 파라미터 불일치

**해결**: `VoteNotificationMetrics` Mock 추가
```java
@Mock
private VoteNotificationMetrics metrics;
```

---

## 3. VotePostServiceTest - 엔티티 ID null 문제
**오류**: `@GeneratedValue` ID가 테스트 환경에서 null 반환
```
expected: 1L but was: null
```

**해결**: ReflectionTestUtils로 ID 직접 설정
```java
ReflectionTestUtils.setField(testVotePost, "id", 1L);
```

---

## 4. VoteNotificationSubscriberTest - DefaultMessage 파라미터 순서 오류
**오류**: 생성자 파라미터 순서가 잘못되어 JSON이 채널명으로 파싱됨
```java
// 잘못된 코드
new DefaultMessage(body, channel.getBytes())
```

**로그 증거**: `channel={"voteId":1,"title":"투표 1"...}`

**해결**: 파라미터 순서 변경 (channel, body)
```java
new DefaultMessage(channel.getBytes(), body)
```

---

## 5. VoteNotificationSchedulerTest - 예외 처리 누락
**오류**: 스케줄러에서 예외 발생 시 전파되어 테스트 실패
```
java.lang.RuntimeException: Simulated failure
```

**해결**: 배치 처리 시 개별 예외 처리 추가
```java
expiredVotes.forEach(vote -> {
    try {
        votePostService.completeVoteAndNotify(vote.getId());
    } catch (Exception e) {
        log.error("투표(ID: {}) 종료 처리 중 예외 발생", vote.getId(), e);
    }
});
```

---

## 6. RedisPubSubIntegrationTest - ApplicationContext 시작 실패
**오류**: JWT secret 길이 부족 및 OAuth2 설정 누락
```
JWT secret must be at least 256 bits
```

**해결**: 테스트용 긴 JWT secret 및 OAuth2 속성 추가
```java
@SpringBootTest(properties = {
    "jwt.secret=testSecretKeyForIntegrationTestPleaseIgnoreThisValue...",
    "spring.security.oauth2.client.registration.kakao.client-id=test",
    "spring.security.oauth2.client.registration.kakao.client-secret=test"
})
```

---

## 7. Actuator Metrics Endpoint - 500 오류
**오류**: `/actuator/metrics` 호출 시 500 Internal Server Error

**원인**: `application.yml`에 management endpoints 설정 누락

**해결**: application-local.yml 및 application-dev.yml에 Actuator 설정 추가
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    metrics:
      enabled: true
```

---

## 테스트 결과
- **총 테스트**: 20개
- **단위 테스트**: 16개 (VotePostService 5개, VoteNotificationSubscriber 5개, VoteNotificationScheduler 6개)
- **통합 테스트**: 4개 (RedisPubSubIntegration)
- **최종 상태**: ✅ 전체 성공
