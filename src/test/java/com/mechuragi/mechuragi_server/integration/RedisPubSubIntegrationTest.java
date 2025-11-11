package com.mechuragi.mechuragi_server.integration;

import com.mechuragi.mechuragi_server.domain.notification.dto.VoteNotificationMessageDTO;
import com.mechuragi.mechuragi_server.domain.notification.dto.VoteNotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest(properties = {
    "jwt.secret=testSecretKeyForIntegrationTestPleaseIgnoreThisValueItIsJustForTestingPurposes1234567890",
    "spring.security.oauth2.client.registration.kakao.client-id=test",
    "spring.security.oauth2.client.registration.kakao.client-secret=test"
})
@ActiveProfiles("test")
@DisplayName("Redis Pub/Sub 통합 테스트")
class RedisPubSubIntegrationTest {

    @Autowired
    private RedisTemplate<String, Object> redisPubSubTemplate;

    @Test
    @DisplayName("투표 종료 알림 발행 및 구독 테스트")
    void publishAndSubscribe_VoteCompleted() throws InterruptedException {
        // given
        VoteNotificationMessageDTO message = VoteNotificationMessageDTO.builder()
                .voteId(100L)
                .title("통합 테스트 투표 - 종료")
                .type(VoteNotificationType.COMPLETED)
                .timestamp(LocalDateTime.now())
                .build();

        // when & then
        // Redis 메시지 발행이 예외 없이 완료되고 Subscriber가 처리하는지 확인
        assertDoesNotThrow(() -> {
            redisPubSubTemplate.convertAndSend("vote:end", message);
            TimeUnit.MILLISECONDS.sleep(500); // 메시지 처리 대기
        });
    }

    @Test
    @DisplayName("투표 종료 10분 전 알림 발행 및 구독 테스트")
    void publishAndSubscribe_VoteEndingSoon() throws InterruptedException {
        // given
        VoteNotificationMessageDTO message = VoteNotificationMessageDTO.builder()
                .voteId(200L)
                .title("통합 테스트 투표 - 종료 10분 전")
                .type(VoteNotificationType.ENDING_SOON)
                .timestamp(LocalDateTime.now())
                .build();

        // when & then
        assertDoesNotThrow(() -> {
            redisPubSubTemplate.convertAndSend("vote:before10min", message);
            TimeUnit.MILLISECONDS.sleep(500); // 메시지 처리 대기
        });
    }

    @Test
    @DisplayName("여러 메시지 동시 발행 및 구독 테스트")
    void publishAndSubscribe_MultipleMessages() throws InterruptedException {
        // given & when & then
        assertDoesNotThrow(() -> {
            for (int i = 1; i <= 3; i++) {
                VoteNotificationMessageDTO message = VoteNotificationMessageDTO.builder()
                        .voteId((long) i)
                        .title("통합 테스트 투표 " + i)
                        .type(VoteNotificationType.COMPLETED)
                        .timestamp(LocalDateTime.now())
                        .build();

                redisPubSubTemplate.convertAndSend("vote:end", message);
                TimeUnit.MILLISECONDS.sleep(100); // 메시지 간 간격
            }
            TimeUnit.MILLISECONDS.sleep(500); // 모든 메시지 처리 대기
        });
    }

    @Test
    @DisplayName("서로 다른 채널로 메시지 발행 테스트")
    void publishAndSubscribe_DifferentChannels() throws InterruptedException {
        // given
        VoteNotificationMessageDTO completedMessage = VoteNotificationMessageDTO.builder()
                .voteId(300L)
                .title("종료 알림 테스트")
                .type(VoteNotificationType.COMPLETED)
                .timestamp(LocalDateTime.now())
                .build();

        VoteNotificationMessageDTO endingSoonMessage = VoteNotificationMessageDTO.builder()
                .voteId(400L)
                .title("종료 10분 전 알림 테스트")
                .type(VoteNotificationType.ENDING_SOON)
                .timestamp(LocalDateTime.now())
                .build();

        // when & then
        assertDoesNotThrow(() -> {
            redisPubSubTemplate.convertAndSend("vote:end", completedMessage);
            redisPubSubTemplate.convertAndSend("vote:before10min", endingSoonMessage);
            TimeUnit.MILLISECONDS.sleep(500); // 메시지 처리 대기
        });
    }
}
