package com.mechuragi.mechuragi_server.domain.notification.service;

import com.mechuragi.mechuragi_server.domain.notification.dto.VoteNotificationMessageDTO;
import com.mechuragi.mechuragi_server.domain.notification.dto.VoteNotificationType;
import com.mechuragi.mechuragi_server.domain.notification.metrics.VoteNotificationMetrics;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoteNotificationService {
    private final SimpMessagingTemplate messagingTemplate;
    private final VoteNotificationMetrics metrics;

    /**
     * STOMP를 통해 특정 사용자에게 알림 전송
     */
    public void sendNotificationToUser(Long memberId, VoteNotificationMessageDTO message) {
        Timer.Sample sample = metrics.startNotificationTimer();

        try {
            String destination = getDestination(message.getType(), memberId);
            messagingTemplate.convertAndSendToUser(
                    memberId.toString(),
                    destination,
                    message
            );

            // 메트릭 기록
            metrics.recordStompMessageSent(destination);
            metrics.recordNotificationSent(message.getType().name());
            metrics.recordNotificationDuration(sample);

            log.info("STOMP 알림 전송 성공: destination={}, voteId={}, memberId={}",
                    destination, message.getVoteId(), memberId);
        } catch (Exception e) {
            metrics.recordNotificationFailed(message.getType().name());
            log.error("STOMP 알림 전송 실패: voteId={}, memberId={}", message.getVoteId(), memberId, e);
        }
    }

    /**
     * 브로드캐스트 방식으로 알림 전송 (하위 호환성 유지)
     */
    @Deprecated
    public void sendNotification(VoteNotificationMessageDTO message) {
        Timer.Sample sample = metrics.startNotificationTimer();

        try {
            String destination = getDestinationBroadcast(message.getType());
            messagingTemplate.convertAndSend(destination, message);

            // 메트릭 기록
            metrics.recordStompMessageSent(destination);
            metrics.recordNotificationSent(message.getType().name());
            metrics.recordNotificationDuration(sample);

            log.info("STOMP 알림 전송 성공: destination={}, voteId={}", destination, message.getVoteId());
        } catch (Exception e) {
            metrics.recordNotificationFailed(message.getType().name());
            log.error("STOMP 알림 전송 실패: voteId={}", message.getVoteId(), e);
        }
    }

    private String getDestination(VoteNotificationType type, Long memberId) {
        return switch (type) {
            case COMPLETED -> "/queue/vote/end";
            case ENDING_SOON -> "/queue/vote/soon";
        };
    }

    private String getDestinationBroadcast(VoteNotificationType type) {
        return switch (type) {
            case COMPLETED -> "/topic/vote/end";
            case ENDING_SOON -> "/topic/vote/soon";
        };
    }
}
