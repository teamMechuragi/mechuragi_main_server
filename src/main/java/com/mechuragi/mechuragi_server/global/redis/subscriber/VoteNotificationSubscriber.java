package com.mechuragi.mechuragi_server.global.redis.subscriber;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mechuragi.mechuragi_server.domain.notification.dto.VoteNotificationMessageDTO;
import com.mechuragi.mechuragi_server.domain.notification.metrics.VoteNotificationMetrics;
import com.mechuragi.mechuragi_server.domain.notification.service.VoteNotificationService;
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
    private final VoteNotificationMetrics metrics;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        long startTime = System.currentTimeMillis();

        try {
            byte[] body = message.getBody();
            VoteNotificationMessageDTO notification = objectMapper.readValue(body, VoteNotificationMessageDTO.class);

            // 메트릭 기록
            metrics.recordRedisMessageReceived(channel);

            log.info("[Redis Subscriber] 메시지 수신: channel={}, voteId={}", channel, notification.getVoteId());

            // 알림 서비스 계층으로 위임
            voteNotificationService.sendNotification(notification);

            long duration = System.currentTimeMillis() - startTime;
            log.info("[Redis Subscriber] 메시지 처리 완료: channel={}, duration={}ms", channel, duration);

        } catch (Exception e) {
            log.error("[Redis Subscriber] 메시지 처리 실패: channel={}, error={}", channel, e.getMessage(), e);
        }
    }
}
