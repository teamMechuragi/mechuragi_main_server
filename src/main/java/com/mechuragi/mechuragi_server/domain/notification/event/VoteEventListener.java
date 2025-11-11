package com.mechuragi.mechuragi_server.domain.notification.event;

import com.mechuragi.mechuragi_server.domain.notification.dto.VoteNotificationMessageDTO;
import com.mechuragi.mechuragi_server.domain.notification.dto.VoteNotificationType;
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
        VoteNotificationMessageDTO message = VoteNotificationMessageDTO.builder()
                .voteId(event.getVoteId())
                .title(event.getTitle())
                .type(VoteNotificationType.COMPLETED)
                .timestamp(LocalDateTime.now())
                .build();

        redisPubSubTemplate.convertAndSend("vote:end", message);
        log.info("투표 종료 알림 발행: voteId={}", event.getVoteId());
    }
}
