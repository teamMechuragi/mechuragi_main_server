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
        try {
            String destination = getDestination(message.getType());
            messagingTemplate.convertAndSend(destination, message);
            log.info("STOMP 알림 전송 성공: destination={}, voteId={}", destination, message.getVoteId());
        } catch (Exception e) {
            log.error("STOMP 알림 전송 실패: voteId={}", message.getVoteId(), e);
        }
    }

    private String getDestination(VoteNotificationType type) {
        return switch (type) {
            case COMPLETED -> "/topic/vote/end";
            case ENDING_SOON -> "/topic/vote/soon";
        };
    }
}
