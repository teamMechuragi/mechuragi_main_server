package com.mechuragi.mechuragi_server.domain.notification.event;

import com.mechuragi.mechuragi_server.domain.member.entity.Member;
import com.mechuragi.mechuragi_server.domain.member.repository.MemberRepository;
import com.mechuragi.mechuragi_server.domain.notification.dto.VoteNotificationMessageDTO;
import com.mechuragi.mechuragi_server.domain.notification.dto.VoteNotificationType;
import com.mechuragi.mechuragi_server.domain.notification.metrics.VoteNotificationMetrics;
import com.mechuragi.mechuragi_server.domain.notification.service.NotificationService;
import io.micrometer.core.instrument.Timer;
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
    private final VoteNotificationMetrics metrics;
    private final NotificationService notificationService;
    private final MemberRepository memberRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleVoteCompleted(VoteCompletedEvent event) {
        Timer.Sample sample = metrics.startRedisPublishTimer();

        try {
            // 투표 작성자 확인 및 알림 설정 확인
            Member author = memberRepository.findById(event.getAuthorId())
                    .orElse(null);

            if (author == null) {
                log.warn("투표 작성자를 찾을 수 없음: authorId={}", event.getAuthorId());
                return;
            }

            // 알림 설정이 꺼져있으면 알림을 보내지 않음
            if (!author.getVoteNotificationEnabled()) {
                log.info("투표 종료 알림 건너뜀 (알림 설정 OFF): voteId={}, authorId={}",
                        event.getVoteId(), event.getAuthorId());
                return;
            }

            // 알림 저장
            notificationService.createNotification(
                    event.getAuthorId(),
                    event.getVoteId(),
                    event.getTitle(),
                    VoteNotificationType.COMPLETED
            );

            // Redis Pub/Sub으로 실시간 알림 발행
            VoteNotificationMessageDTO message = VoteNotificationMessageDTO.builder()
                    .voteId(event.getVoteId())
                    .title(event.getTitle())
                    .type(VoteNotificationType.COMPLETED)
                    .timestamp(LocalDateTime.now())
                    .memberId(event.getAuthorId())
                    .build();

            redisPubSubTemplate.convertAndSend("vote:end", message);
            metrics.recordRedisPublishDuration(sample);

            log.info("투표 종료 알림 발행: voteId={}, authorId={}", event.getVoteId(), event.getAuthorId());
        } catch (Exception e) {
            log.error("투표 종료 알림 발행 실패: voteId={}", event.getVoteId(), e);
        }
    }
}
