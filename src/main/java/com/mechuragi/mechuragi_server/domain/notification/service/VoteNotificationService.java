package com.mechuragi.mechuragi_server.domain.notification.service;

import com.mechuragi.mechuragi_server.domain.notification.dto.VoteNotificationMessageDTO;
import com.mechuragi.mechuragi_server.domain.notification.dto.VoteNotificationType;
import com.mechuragi.mechuragi_server.domain.notification.metrics.VoteNotificationMetrics;
import com.mechuragi.mechuragi_server.global.sse.SseEmitterRepository;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoteNotificationService {

    private final SseEmitterRepository sseEmitterRepository;
    private final VoteNotificationMetrics metrics;

    /**
     * SSE를 통해 특정 사용자에게 알림 전송
     */
    public void sendNotificationToUser(Long memberId, VoteNotificationMessageDTO message) {
        Timer.Sample sample = metrics.startNotificationTimer();

        SseEmitter emitter = sseEmitterRepository.findById(memberId);
        if (emitter == null) {
            log.debug("[SSE] 연결된 Emitter 없음: memberId={}", memberId);
            metrics.recordNotificationDuration(sample);
            return;
        }

        String eventName = getEventName(message.getType());

        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(message));

            metrics.recordSseMessageSent(eventName);
            metrics.recordNotificationSent(message.getType().name());
            metrics.recordNotificationDuration(sample);

            log.info("[SSE] 알림 전송 성공: event={}, voteId={}, memberId={}",
                    eventName, message.getVoteId(), memberId);
        } catch (IOException e) {
            metrics.recordNotificationFailed(message.getType().name());
            log.warn("[SSE] 알림 전송 실패 (연결 끊김): memberId={}, error={}", memberId, e.getMessage());
            sseEmitterRepository.deleteById(memberId);
        } catch (Exception e) {
            metrics.recordNotificationFailed(message.getType().name());
            log.error("[SSE] 알림 전송 실패: voteId={}, memberId={}", message.getVoteId(), memberId, e);
        }
    }

    /**
     * 브로드캐스트 방식 (SSE에서는 사용하지 않음 - 하위 호환성 유지)
     */
    @Deprecated
    public void sendNotification(VoteNotificationMessageDTO message) {
        log.warn("[SSE] 브로드캐스트 방식은 SSE에서 지원되지 않습니다. memberId를 지정하세요.");
    }

    private String getEventName(VoteNotificationType type) {
        return switch (type) {
            case COMPLETED -> "vote-end";
            case ENDING_SOON -> "vote-soon";
        };
    }
}
