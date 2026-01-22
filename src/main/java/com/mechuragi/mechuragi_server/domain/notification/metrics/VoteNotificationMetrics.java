package com.mechuragi.mechuragi_server.domain.notification.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 투표 알림 시스템의 메트릭을 수집하는 클래스
 */
@Slf4j
@Component
public class VoteNotificationMetrics {

    private final MeterRegistry registry;
    private final Counter notificationSentCounter;
    private final Counter notificationFailedCounter;
    private final Counter redisMessageReceivedCounter;
    private final Counter sseMessageSentCounter;
    private final Timer notificationProcessingTimer;
    private final Timer redisPublishTimer;

    public VoteNotificationMetrics(MeterRegistry registry) {
        this.registry = registry;

        // 알림 발송 성공 카운터
        this.notificationSentCounter = Counter.builder("vote.notification.sent")
                .description("투표 알림 발송 성공 횟수")
                .tag("type", "total")
                .register(registry);

        // 알림 발송 실패 카운터
        this.notificationFailedCounter = Counter.builder("vote.notification.failed")
                .description("투표 알림 발송 실패 횟수")
                .tag("type", "total")
                .register(registry);

        // Redis 메시지 수신 카운터
        this.redisMessageReceivedCounter = Counter.builder("vote.redis.message.received")
                .description("Redis Pub/Sub 메시지 수신 횟수")
                .register(registry);

        // SSE 메시지 발송 카운터
        this.sseMessageSentCounter = Counter.builder("vote.sse.message.sent")
                .description("SSE를 통한 메시지 발송 횟수")
                .register(registry);

        // 알림 처리 시간 타이머
        this.notificationProcessingTimer = Timer.builder("vote.notification.duration")
                .description("투표 알림 처리 시간")
                .register(registry);

        // Redis 발행 시간 타이머
        this.redisPublishTimer = Timer.builder("vote.redis.publish.duration")
                .description("Redis Pub/Sub 메시지 발행 시간")
                .register(registry);
    }

    /**
     * 알림 발송 성공 기록
     */
    public void recordNotificationSent() {
        notificationSentCounter.increment();
        log.debug("[Metrics] 알림 발송 성공 카운트 증가");
    }

    /**
     * 알림 발송 성공 기록 (타입별)
     * @param notificationType 알림 타입 (COMPLETED, ENDING_SOON)
     */
    public void recordNotificationSent(String notificationType) {
        Counter.builder("vote.notification.sent")
                .description("투표 알림 발송 성공 횟수 (타입별)")
                .tag("notification_type", notificationType)
                .register(registry)
                .increment();

        notificationSentCounter.increment();
        log.debug("[Metrics] 알림 발송 성공: type={}", notificationType);
    }

    /**
     * 알림 발송 실패 기록
     */
    public void recordNotificationFailed() {
        notificationFailedCounter.increment();
        log.warn("[Metrics] 알림 발송 실패 카운트 증가");
    }

    /**
     * 알림 발송 실패 기록 (타입별)
     * @param notificationType 알림 타입
     */
    public void recordNotificationFailed(String notificationType) {
        Counter.builder("vote.notification.failed")
                .description("투표 알림 발송 실패 횟수 (타입별)")
                .tag("notification_type", notificationType)
                .register(registry)
                .increment();

        notificationFailedCounter.increment();
        log.warn("[Metrics] 알림 발송 실패: type={}", notificationType);
    }

    /**
     * Redis 메시지 수신 기록
     */
    public void recordRedisMessageReceived() {
        redisMessageReceivedCounter.increment();
        log.debug("[Metrics] Redis 메시지 수신");
    }

    /**
     * Redis 메시지 수신 기록 (채널별)
     * @param channel Redis 채널명
     */
    public void recordRedisMessageReceived(String channel) {
        Counter.builder("vote.redis.message.received")
                .description("Redis Pub/Sub 메시지 수신 횟수 (채널별)")
                .tag("channel", channel)
                .register(registry)
                .increment();

        redisMessageReceivedCounter.increment();
        log.debug("[Metrics] Redis 메시지 수신: channel={}", channel);
    }

    /**
     * SSE 메시지 발송 기록
     */
    public void recordSseMessageSent() {
        sseMessageSentCounter.increment();
        log.debug("[Metrics] SSE 메시지 발송");
    }

    /**
     * SSE 메시지 발송 기록 (이벤트별)
     * @param eventName SSE 이벤트명
     */
    public void recordSseMessageSent(String eventName) {
        Counter.builder("vote.sse.message.sent")
                .description("SSE를 통한 메시지 발송 횟수 (이벤트별)")
                .tag("event", eventName)
                .register(registry)
                .increment();

        sseMessageSentCounter.increment();
        log.debug("[Metrics] SSE 메시지 발송: event={}", eventName);
    }

    /**
     * 알림 처리 시간 기록
     * @return Timer.Sample - 시간 측정을 위한 샘플
     */
    public Timer.Sample startNotificationTimer() {
        return Timer.start();
    }

    /**
     * 알림 처리 시간 종료 및 기록
     * @param sample 시작 시간 샘플
     */
    public void recordNotificationDuration(Timer.Sample sample) {
        sample.stop(notificationProcessingTimer);
        log.debug("[Metrics] 알림 처리 시간 기록");
    }

    /**
     * Redis 발행 시간 기록 시작
     * @return Timer.Sample
     */
    public Timer.Sample startRedisPublishTimer() {
        return Timer.start();
    }

    /**
     * Redis 발행 시간 종료 및 기록
     * @param sample 시작 시간 샘플
     */
    public void recordRedisPublishDuration(Timer.Sample sample) {
        sample.stop(redisPublishTimer);
        log.debug("[Metrics] Redis 발행 시간 기록");
    }

    /**
     * 현재 메트릭 통계 로그 출력
     */
    public void logMetricsSummary() {
        log.info("[Metrics Summary] 알림 발송 성공: {}, 실패: {}, Redis 수신: {}, SSE 발송: {}",
                notificationSentCounter.count(),
                notificationFailedCounter.count(),
                redisMessageReceivedCounter.count(),
                sseMessageSentCounter.count());
    }
}
