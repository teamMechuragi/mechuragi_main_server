# 투표 종료 및 종료 10분 전 실시간 알림 시스템

## 📋 목차
1. [개요](#개요)
2. [시스템 아키텍처](#시스템-아키텍처)
3. [현재 프로젝트 현황](#현재-프로젝트-현황)
4. [구현 단계](#구현-단계)
5. [메시지 포맷 및 채널 정의](#메시지-포맷-및-채널-정의)
6. [주의사항 및 베스트 프랙티스](#주의사항-및-베스트-프랙티스)
7. [테스트 가이드](#테스트-가이드)
8. [모니터링 및 에러 처리](#모니터링-및-에러-처리)

---

## 개요

### 목적
투표 데이터를 **MySQL (JPA)**에 저장하고, 투표 종료 알림을 **Redis Pub/Sub**으로 실시간 전파하여 **WebSocket (STOMP)**을 통해 클라이언트에게 즉시 전달합니다.

### 핵심 이점
- **DB 부하 감소**: MySQL에 실시간 폴링하지 않고 Redis를 통한 이벤트 기반 알림
- **분산 환경 지원**: 여러 서버 인스턴스가 동일한 Redis를 통해 이벤트 공유
- **확장 가능성**: 이메일, 푸시 알림 등 다양한 채널로 확장 용이
- **낮은 지연시간**: Redis Pub/Sub의 빠른 메시지 전파

### 알림 종류
1. **투표 종료 10분 전 알림**: 사용자에게 투표 마감 임박 알림
2. **투표 종료 알림**: 투표가 종료되었음을 실시간 전달

---

## 시스템 아키텍처

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Scheduler  │────▶│   Service    │────▶│ Redis Pub/Sub│────▶│  Subscriber  │
│  (1분마다)   │     │ (Publisher)  │     │   Channel    │     │  (Listener)  │
└──────────────┘     └──────────────┘     └──────────────┘     └──────────────┘
                            │                                            │
                            ▼                                            ▼
                     ┌──────────────┐                          ┌──────────────┐
                     │    MySQL     │                          │   STOMP      │
                     │   (JPA DB)   │                          │  (WebSocket) │
                     └──────────────┘                          └──────────────┘
                                                                         │
                                                                         ▼
                                                                ┌──────────────┐
                                                                │   Client     │
                                                                │  (Frontend)  │
                                                                └──────────────┘
```

### 데이터 흐름
1. **스케줄러** (1분마다 실행)
   - 투표 종료 10분 전인 투표 검색
   - 투표 종료 시간이 지난 투표 검색

2. **서비스 계층** (Publisher)
   - VotePost 상태를 `COMPLETED`로 업데이트
   - Redis 채널로 알림 메시지 발행

3. **Redis Pub/Sub**
   - 메시지를 모든 구독자에게 즉시 전파

4. **Subscriber**
   - Redis 메시지 수신
   - 알림 서비스 계층으로 전달

5. **STOMP**
   - WebSocket을 통해 클라이언트에게 실시간 전송

---

## 현재 프로젝트 현황

### ✅ 이미 구현된 것
- **Redis 설정**: `RedisCacheConfig.java`, `RedisDataInitializer.java`
- **RedisTemplate**: String 직렬화 및 JSON 직렬화 설정 완료
- **Vote Entity**: `VotePost`, `VoteOption`, `VoteParticipation` 등
- **Vote Service**: `VotePostService`, `VoteParticipationService` 등
- **Scheduler 설정**: `@EnableScheduling` 활성화 (`PopularMenuScheduler` 존재)
- **Vote Repository**: `VotePostRepository` 등

### ❌ 새로 구현해야 할 것
- **WebSocket/STOMP 설정**
- **Redis Pub/Sub Listener 설정**
- **VoteNotificationService** (알림 처리 중간 계층)
- **투표 종료 스케줄러**
- **투표 종료 10분 전 스케줄러**
- **Repository 쿼리 메서드** (10분 전 투표 검색용)

---

## 구현 단계

### 4.1 의존성 추가

#### build.gradle
```gradle
dependencies {
    // 기존 의존성...

    // WebSocket & STOMP (새로 추가)
    implementation 'org.springframework.boot:spring-boot-starter-websocket'

    // Redis는 이미 추가되어 있음
    // implementation 'org.springframework.boot:spring-boot-starter-data-redis'
}
```

---

### 4.2 WebSocket/STOMP 설정

#### 파일 위치
`src/main/java/com/mechuragi/mechuragi_server/global/config/WebSocketConfig.java`

#### 코드
```java
package com.mechuragi.mechuragi_server.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 클라이언트로 메시지를 전송할 때 사용할 prefix
        config.enableSimpleBroker("/topic", "/queue");

        // 클라이언트에서 서버로 메시지를 보낼 때 사용할 prefix
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 연결 엔드포인트
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // CORS 설정 (프로덕션에서는 제한 필요)
                .withSockJS(); // SockJS fallback
    }
}
```

---

### 4.3 Redis Pub/Sub Publisher 설정

#### 4.3.1 RedisCacheConfig 수정

기존 `RedisCacheConfig.java`에 Pub/Sub용 RedisTemplate 추가

**파일 위치**: `src/main/java/com/mechuragi/mechuragi_server/global/redis/publisher/RedisCacheConfig.java`

```java
// 기존 코드에 추가
@Bean
public RedisTemplate<String, Object> redisPubSubTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);

    // Key: String 직렬화
    template.setKeySerializer(new StringRedisSerializer());

    // Value: JSON 직렬화 (Pub/Sub 메시지용)
    GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper());
    template.setValueSerializer(serializer);
    template.setHashValueSerializer(serializer);

    return template;
}
```

#### 4.3.2 VotePostService에 발행 로직 추가

**파일 위치**: `src/main/java/com/mechuragi/mechuragi_server/domain/vote/service/VotePostService.java`

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VotePostService {
    private final VotePostRepository votePostRepository;
    private final RedisTemplate<String, Object> redisPubSubTemplate; // 추가
    private final ApplicationEventPublisher eventPublisher; // 추가

    // 기존 코드...

    /**
     * 투표 종료 처리 및 Redis 발행
     */
    @Transactional
    public void completeVoteAndNotify(Long voteId) {
        VotePost votePost = votePostRepository.findById(voteId)
                .orElseThrow(() -> new IllegalArgumentException("투표를 찾을 수 없습니다."));

        // 투표 상태 변경
        votePost.completeVote();
        votePostRepository.save(votePost);

        // 트랜잭션 커밋 후 이벤트 발행
        eventPublisher.publishEvent(new VoteCompletedEvent(votePost.getId(), votePost.getTitle()));
    }

    /**
     * 투표 종료 10분 전 알림 발행
     */
    public void notifyVoteEndingSoon(Long voteId, String title) {
        VoteNotificationMessage message = VoteNotificationMessage.builder()
                .voteId(voteId)
                .title(title)
                .type(VoteNotificationType.ENDING_SOON)
                .timestamp(LocalDateTime.now())
                .build();

        redisPubSubTemplate.convertAndSend("vote:before10min", message);
    }
}
```

#### 4.3.3 이벤트 클래스 생성

**파일 위치**: `src/main/java/com/mechuragi/mechuragi_server/domain/vote/event/VoteCompletedEvent.java`

```java
package com.mechuragi.mechuragi_server.domain.vote.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class VoteCompletedEvent {
    private final Long voteId;
    private final String title;
}
```

#### 4.3.4 이벤트 리스너 생성

**파일 위치**: `src/main/java/com/mechuragi/mechuragi_server/domain/vote/event/VoteEventListener.java`

```java
package com.mechuragi.mechuragi_server.domain.vote.event;

import com.mechuragi.mechuragi_server.domain.vote.dto.VoteNotificationMessage;
import com.mechuragi.mechuragi_server.domain.vote.dto.VoteNotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class VoteEventListener {
    private final RedisTemplate<String, Object> redisPubSubTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleVoteCompleted(VoteCompletedEvent event) {
        VoteNotificationMessage message = VoteNotificationMessage.builder()
                .voteId(event.getVoteId())
                .title(event.getTitle())
                .type(VoteNotificationType.COMPLETED)
                .timestamp(LocalDateTime.now())
                .build();

        redisPubSubTemplate.convertAndSend("vote:end", message);
        log.info("투표 종료 알림 발행: voteId={}", event.getVoteId());
    }
}
```

---

### 4.4 Redis Pub/Sub Subscriber 설정

#### 4.4.1 Subscriber 클래스 생성

**파일 위치**: `src/main/java/com/mechuragi/mechuragi_server/global/redis/subscriber/VoteNotificationSubscriber.java`

```java
package com.mechuragi.mechuragi_server.global.redis.subscriber;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mechuragi.mechuragi_server.domain.vote.dto.VoteNotificationMessage;
import com.mechuragi.mechuragi_server.domain.vote.service.VoteNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VoteNotificationSubscriber implements MessageListener {
    private final VoteNotificationService voteNotificationService;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            byte[] body = message.getBody();

            VoteNotificationMessage notification = objectMapper.readValue(body, VoteNotificationMessage.class);

            log.info("Redis 메시지 수신: channel={}, voteId={}", channel, notification.getVoteId());

            // 알림 서비스 계층으로 위임
            voteNotificationService.sendNotification(notification);

        } catch (Exception e) {
            log.error("Redis 메시지 처리 실패", e);
        }
    }
}
```

#### 4.4.2 리스너 컨테이너 설정

**파일 위치**: `src/main/java/com/mechuragi/mechuragi_server/global/redis/subscriber/RedisSubscriberConfig.java`

```java
package com.mechuragi.mechuragi_server.global.redis.subscriber;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
@RequiredArgsConstructor
public class RedisSubscriberConfig {
    private final VoteNotificationSubscriber voteNotificationSubscriber;

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // 투표 종료 채널 구독
        container.addMessageListener(
                new MessageListenerAdapter(voteNotificationSubscriber),
                new PatternTopic("vote:end")
        );

        // 투표 종료 10분 전 채널 구독
        container.addMessageListener(
                new MessageListenerAdapter(voteNotificationSubscriber),
                new PatternTopic("vote:before10min")
        );

        return container;
    }
}
```

---

### 4.5 알림 서비스 계층 구현

#### 4.5.1 VoteNotificationService 생성

**파일 위치**: `src/main/java/com/mechuragi/mechuragi_server/domain/vote/service/VoteNotificationService.java`

```java
package com.mechuragi.mechuragi_server.domain.vote.service;

import com.mechuragi.mechuragi_server.domain.vote.dto.VoteNotificationMessage;
import com.mechuragi.mechuragi_server.domain.vote.dto.VoteNotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoteNotificationService {
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * STOMP를 통해 클라이언트에게 알림 전송
     */
    public void sendNotification(VoteNotificationMessage message) {
        String destination = getDestination(message.getType());

        messagingTemplate.convertAndSend(destination, message);
        log.info("STOMP 알림 전송: destination={}, voteId={}", destination, message.getVoteId());
    }

    private String getDestination(VoteNotificationType type) {
        return switch (type) {
            case COMPLETED -> "/topic/vote/end";
            case ENDING_SOON -> "/topic/vote/soon";
        };
    }
}
```

#### 4.5.2 DTO 클래스 생성

**파일 위치**: `src/main/java/com/mechuragi/mechuragi_server/domain/vote/dto/VoteNotificationMessage.java`

```java
package com.mechuragi.mechuragi_server.domain.vote.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoteNotificationMessage {
    private Long voteId;
    private String title;
    private VoteNotificationType type;
    private LocalDateTime timestamp;
}
```

**파일 위치**: `src/main/java/com/mechuragi/mechuragi_server/domain/vote/dto/VoteNotificationType.java`

```java
package com.mechuragi.mechuragi_server.domain.vote.dto;

public enum VoteNotificationType {
    COMPLETED,      // 투표 종료
    ENDING_SOON     // 종료 10분 전
}
```

---

### 4.6 스케줄러 구현

#### 4.6.1 VotePostRepository에 쿼리 메서드 추가

**파일 위치**: `src/main/java/com/mechuragi/mechuragi_server/domain/vote/repository/VotePostRepository.java`

```java
public interface VotePostRepository extends JpaRepository<VotePost, Long> {
    // 기존 메서드...

    /**
     * 투표 종료 10분 전 투표 검색
     * @param tenMinutesLater 현재 시간 + 10분
     * @param elevenMinutesLater 현재 시간 + 11분
     */
    @Query("SELECT v FROM VotePost v WHERE v.status = 'ACTIVE' " +
           "AND v.deadline BETWEEN :tenMinutesLater AND :elevenMinutesLater")
    List<VotePost> findVotesEndingInTenMinutes(
            @Param("tenMinutesLater") LocalDateTime tenMinutesLater,
            @Param("elevenMinutesLater") LocalDateTime elevenMinutesLater
    );

    /**
     * 만료된 투표 검색 (기존 메서드 활용 가능)
     */
    @Query("SELECT v FROM VotePost v WHERE v.status = 'ACTIVE' AND v.deadline < :now")
    List<VotePost> findExpiredActiveVotes(@Param("now") LocalDateTime now);
}
```

#### 4.6.2 스케줄러 생성

**파일 위치**: `src/main/java/com/mechuragi/mechuragi_server/global/scheduler/VoteNotificationScheduler.java`

```java
package com.mechuragi.mechuragi_server.global.scheduler;

import com.mechuragi.mechuragi_server.domain.vote.entity.VotePost;
import com.mechuragi.mechuragi_server.domain.vote.repository.VotePostRepository;
import com.mechuragi.mechuragi_server.domain.vote.service.VotePostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class VoteNotificationScheduler {
    private final VotePostRepository votePostRepository;
    private final VotePostService votePostService;

    /**
     * 투표 종료 10분 전 알림 스케줄러
     * 매 1분마다 실행
     */
    @Scheduled(cron = "0 * * * * *") // 매 분 0초에 실행
    public void notifyVotesEndingSoon() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tenMinutesLater = now.plusMinutes(10);
        LocalDateTime elevenMinutesLater = now.plusMinutes(11);

        List<VotePost> endingSoonVotes = votePostRepository.findVotesEndingInTenMinutes(
                tenMinutesLater, elevenMinutesLater
        );

        if (!endingSoonVotes.isEmpty()) {
            log.info("투표 종료 10분 전 알림 발송: {} 건", endingSoonVotes.size());

            endingSoonVotes.forEach(vote -> {
                votePostService.notifyVoteEndingSoon(vote.getId(), vote.getTitle());
            });
        }
    }

    /**
     * 만료된 투표 종료 처리 스케줄러
     * 매 1분마다 실행
     */
    @Scheduled(cron = "0 * * * * *") // 매 분 0초에 실행
    public void completeExpiredVotes() {
        LocalDateTime now = LocalDateTime.now();
        List<VotePost> expiredVotes = votePostRepository.findExpiredActiveVotes(now);

        if (!expiredVotes.isEmpty()) {
            log.info("만료된 투표 종료 처리: {} 건", expiredVotes.size());

            expiredVotes.forEach(vote -> {
                votePostService.completeVoteAndNotify(vote.getId());
            });
        }
    }
}
```

---

## 메시지 포맷 및 채널 정의

### Redis 채널 네이밍

| 채널 이름 | 용도 | STOMP 토픽 매핑 |
|----------|------|-----------------|
| `vote:end` | 투표 종료 알림 | `/topic/vote/end` |
| `vote:before10min` | 투표 종료 10분 전 알림 | `/topic/vote/soon` |

### 메시지 포맷

```json
{
  "voteId": 123,
  "title": "점심 메뉴 투표",
  "type": "COMPLETED",
  "timestamp": "2025-11-11T14:30:00"
}
```

### 클라이언트 구독 예시 (JavaScript)

```javascript
// SockJS + STOMP 연결
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    console.log('Connected: ' + frame);

    // 투표 종료 알림 구독
    stompClient.subscribe('/topic/vote/end', function(message) {
        const notification = JSON.parse(message.body);
        console.log('투표 종료:', notification);
        // UI 업데이트 로직
    });

    // 투표 종료 10분 전 알림 구독
    stompClient.subscribe('/topic/vote/soon', function(message) {
        const notification = JSON.parse(message.body);
        console.log('투표 종료 임박:', notification);
        // UI 업데이트 로직
    });
});
```

---

## 주의사항 및 베스트 프랙티스

### 1. Serializer 통일
- **RedisTemplate**과 **MessageListener**에서 동일한 `GenericJackson2JsonRedisSerializer` 사용
- JSON 파싱 오류 방지 및 메시지 일관성 유지
- ObjectMapper 설정을 공유하여 LocalDateTime 등의 직렬화 일관성 보장

### 2. 스케줄러 분리 및 효율화
- 종료 10분 전 / 종료 시점 **각각 별도 스케줄러** 운영
- `@Scheduled(cron="0 * * * * *")`: 매 분 0초에 실행
- **deadline 필드에 인덱스 추가** 권장 (full-scan 방지)
  ```sql
  CREATE INDEX idx_votepost_deadline_status ON vote_post(deadline, status);
  ```

### 3. 트랜잭션 이후 메시지 발행
- `@TransactionalEventListener(phase = AFTER_COMMIT)` 사용
- **DB 반영 실패 시 잘못된 알림 방지**
- 트랜잭션 롤백 시 Redis 메시지도 발행되지 않음

### 4. Subscriber → STOMP 직접 연결 금지
- **중간 계층 VoteNotificationService** 두어 알림 처리 분리
- 향후 이메일, 푸시 등 **알림 채널 확장에 유리**
- 단일 책임 원칙 준수

### 5. 채널 네이밍 규칙
- 이벤트 단위로 채널 분리: `vote:end`, `vote:before10min`
- STOMP 토픽과 명확한 매핑 가능
- 확장 시 일관된 네이밍 유지 (`vote:created`, `vote:updated` 등)

### 6. 에러 핸들링
- Redis 연결 실패 시 재시도 로직 고려
- 메시지 파싱 실패 시 로그 남기고 계속 진행
- STOMP 전송 실패 시에도 서비스 중단 방지

### 7. 중복 알림 방지
- 스케줄러가 1분 단위로 실행되므로 **이미 알림 발송된 투표는 제외** 필요
- 옵션 1: VotePost에 `notifiedBefore10Min` 플래그 추가
- 옵션 2: Redis Set에 알림 발송 이력 저장 (TTL 15분)

---

## 테스트 가이드

### 1. 단위 테스트

#### VotePostServiceTest
```java
@Test
void 투표_종료_처리_및_이벤트_발행() {
    // given
    VotePost votePost = createVotePost();
    when(votePostRepository.findById(1L)).thenReturn(Optional.of(votePost));

    // when
    votePostService.completeVoteAndNotify(1L);

    // then
    assertEquals(VoteStatus.COMPLETED, votePost.getStatus());
    verify(eventPublisher).publishEvent(any(VoteCompletedEvent.class));
}
```

#### VoteNotificationSubscriberTest
```java
@Test
void Redis_메시지_수신_처리() throws Exception {
    // given
    VoteNotificationMessage message = VoteNotificationMessage.builder()
            .voteId(1L)
            .title("테스트 투표")
            .type(VoteNotificationType.COMPLETED)
            .timestamp(LocalDateTime.now())
            .build();

    byte[] body = objectMapper.writeValueAsBytes(message);
    Message redisMessage = new DefaultMessage(body, "vote:end".getBytes());

    // when
    subscriber.onMessage(redisMessage, null);

    // then
    verify(voteNotificationService).sendNotification(any());
}
```

### 2. 통합 테스트

#### Redis Pub/Sub 통합 테스트
```java
@SpringBootTest
@TestPropertySource(properties = "spring.redis.host=localhost")
class RedisPubSubIntegrationTest {

    @Autowired
    private RedisTemplate<String, Object> redisPubSubTemplate;

    @Autowired
    private VoteNotificationService notificationService;

    @Test
    void Redis_발행_및_구독_테스트() throws Exception {
        // given
        VoteNotificationMessage message = VoteNotificationMessage.builder()
                .voteId(1L)
                .title("통합 테스트 투표")
                .type(VoteNotificationType.COMPLETED)
                .timestamp(LocalDateTime.now())
                .build();

        // when
        redisPubSubTemplate.convertAndSend("vote:end", message);

        // then
        Thread.sleep(1000); // 메시지 전파 대기
        verify(notificationService, timeout(2000)).sendNotification(any());
    }
}
```

### 3. 스케줄러 테스트

```java
@Test
void 만료된_투표_종료_스케줄러() {
    // given
    VotePost expiredVote = createExpiredVotePost();
    when(votePostRepository.findExpiredActiveVotes(any()))
            .thenReturn(List.of(expiredVote));

    // when
    scheduler.completeExpiredVotes();

    // then
    verify(votePostService).completeVoteAndNotify(expiredVote.getId());
}
```

### 4. E2E 테스트 (수동)

1. **서버 시작**
2. **투표 생성** (마감 시간을 현재 + 11분으로 설정)
3. **WebSocket 연결** (프론트엔드 또는 테스트 클라이언트)
4. **1분 대기** 후 10분 전 알림 확인
5. **11분 대기** 후 종료 알림 확인

---

## 모니터링 및 에러 처리

### 1. 로깅 전략

```java
@Slf4j
public class VoteNotificationSubscriber {
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        long startTime = System.currentTimeMillis();

        try {
            // 메시지 처리
            log.info("[Redis Subscriber] 메시지 수신 시작: channel={}", channel);

            // ... 처리 로직 ...

            long duration = System.currentTimeMillis() - startTime;
            log.info("[Redis Subscriber] 메시지 처리 완료: channel={}, duration={}ms",
                     channel, duration);
        } catch (Exception e) {
            log.error("[Redis Subscriber] 메시지 처리 실패: channel={}, error={}",
                      channel, e.getMessage(), e);
        }
    }
}
```

### 2. 에러 처리

#### Redis 연결 실패
```java
@Component
public class RedisHealthChecker {
    @Autowired
    private RedisConnectionFactory connectionFactory;

    @Scheduled(fixedRate = 60000) // 1분마다
    public void checkRedisConnection() {
        try {
            connectionFactory.getConnection().ping();
            log.info("Redis 연결 정상");
        } catch (Exception e) {
            log.error("Redis 연결 실패", e);
            // 알림 발송 (슬랙, 이메일 등)
        }
    }
}
```

#### STOMP 전송 실패
```java
public void sendNotification(VoteNotificationMessage message) {
    try {
        String destination = getDestination(message.getType());
        messagingTemplate.convertAndSend(destination, message);
        log.info("STOMP 알림 전송 성공: voteId={}", message.getVoteId());
    } catch (Exception e) {
        log.error("STOMP 알림 전송 실패: voteId={}", message.getVoteId(), e);
        // 재시도 로직 또는 fallback 처리
    }
}
```

### 3. 메트릭 수집

```java
@Component
public class VoteNotificationMetrics {
    private final Counter notificationSentCounter;
    private final Counter notificationFailedCounter;
    private final Timer notificationTimer;

    public VoteNotificationMetrics(MeterRegistry registry) {
        this.notificationSentCounter = registry.counter("vote.notification.sent");
        this.notificationFailedCounter = registry.counter("vote.notification.failed");
        this.notificationTimer = registry.timer("vote.notification.duration");
    }

    public void recordSuccess() {
        notificationSentCounter.increment();
    }

    public void recordFailure() {
        notificationFailedCounter.increment();
    }
}
```

### 4. 알림 모니터링 대시보드

**주요 지표:**
- 시간당 발송된 알림 수
- 알림 전송 실패율
- Redis Pub/Sub 지연시간
- STOMP 연결 수
- 스케줄러 실행 횟수 및 처리 건수

---

## 구현 체크리스트

- [ ] WebSocket/STOMP 설정 (`WebSocketConfig.java`)
- [ ] Redis Pub/Sub용 RedisTemplate 추가
- [ ] VoteNotificationMessage DTO 생성
- [ ] VoteNotificationType Enum 생성
- [ ] VoteCompletedEvent 생성
- [ ] VoteEventListener 생성
- [ ] VoteNotificationSubscriber 생성
- [ ] RedisSubscriberConfig 생성
- [ ] VoteNotificationService 생성
- [ ] VotePostService에 발행 로직 추가
- [ ] VotePostRepository에 쿼리 메서드 추가
- [ ] VoteNotificationScheduler 생성
- [ ] deadline 필드 인덱스 추가
- [ ] 단위 테스트 작성
- [ ] 통합 테스트 작성
- [ ] E2E 테스트 (수동)
- [ ] 로깅 및 모니터링 설정

---

## 참고 자료

### 프로젝트 내 참고 파일
- `src/main/java/com/mechuragi/mechuragi_server/global/redis/publisher/RedisCacheConfig.java`
- `src/main/java/com/mechuragi/mechuragi_server/global/scheduler/PopularMenuScheduler.java`
- `src/main/java/com/mechuragi/mechuragi_server/domain/vote/service/VotePostService.java`
- `src/main/java/com/mechuragi/mechuragi_server/domain/vote/entity/VotePost.java`

### 외부 문서
- [Spring WebSocket Documentation](https://docs.spring.io/spring-framework/reference/web/websocket.html)
- [Spring Data Redis - Pub/Sub](https://docs.spring.io/spring-data/redis/reference/redis/pubsub.html)
- [STOMP Protocol](https://stomp.github.io/)
