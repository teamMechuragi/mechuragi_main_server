package com.mechuragi.mechuragi_server.integration;

import com.mechuragi.mechuragi_server.domain.notification.dto.VoteNotificationMessageDTO;
import com.mechuragi.mechuragi_server.domain.notification.dto.VoteNotificationType;
import com.mechuragi.mechuragi_server.domain.notification.service.VoteNotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Redis Pub/Sub 통합 테스트")
class RedisPubSubIntegrationTest {

    @Autowired
    private RedisTemplate<String, Object> redisPubSubTemplate;

    @SpyBean
    private VoteNotificationService voteNotificationService;

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

        // when
        redisPubSubTemplate.convertAndSend("vote:end", message);

        // then
        // 최대 2초 동안 대기하며 메시지가 전달되었는지 확인
        verify(voteNotificationService, timeout(2000).times(1))
                .sendNotification(any(VoteNotificationMessageDTO.class));
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

        // when
        redisPubSubTemplate.convertAndSend("vote:before10min", message);

        // then
        verify(voteNotificationService, timeout(2000).times(1))
                .sendNotification(any(VoteNotificationMessageDTO.class));
    }

    @Test
    @DisplayName("여러 메시지 동시 발행 및 구독 테스트")
    void publishAndSubscribe_MultipleMessages() throws InterruptedException {
        // given & when
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

        // then
        verify(voteNotificationService, timeout(3000).times(3))
                .sendNotification(any(VoteNotificationMessageDTO.class));
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

        // when
        redisPubSubTemplate.convertAndSend("vote:end", completedMessage);
        redisPubSubTemplate.convertAndSend("vote:before10min", endingSoonMessage);

        // then
        verify(voteNotificationService, timeout(2000).times(2))
                .sendNotification(any(VoteNotificationMessageDTO.class));
    }
}
