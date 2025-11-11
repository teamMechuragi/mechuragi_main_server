package com.mechuragi.mechuragi_server.global.redis.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Redis 연결 상태를 주기적으로 확인하는 헬스 체커
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisHealthChecker {

    private final RedisConnectionFactory connectionFactory;
    private volatile boolean lastHealthStatus = true;

    /**
     * Redis 연결 상태 확인
     * 1분마다 실행
     */
    @Scheduled(fixedRate = 60000) // 60초마다
    public void checkRedisConnection() {
        try {
            String pong = connectionFactory.getConnection().ping();

            if ("PONG".equals(pong)) {
                if (!lastHealthStatus) {
                    log.info("[Redis Health Check] Redis 연결 복구됨");
                    lastHealthStatus = true;
                } else {
                    log.debug("[Redis Health Check] Redis 연결 정상");
                }
            }
        } catch (Exception e) {
            if (lastHealthStatus) {
                log.error("[Redis Health Check] Redis 연결 실패 - 알림 시스템이 동작하지 않을 수 있습니다.", e);
                lastHealthStatus = false;
                // TODO: 슬랙, 이메일 등 외부 알림 발송
                // notificationService.sendAlert("Redis 연결 실패", e.getMessage());
            } else {
                log.warn("[Redis Health Check] Redis 연결 실패 (계속됨): {}", e.getMessage());
            }
        }
    }

    /**
     * 현재 Redis 연결 상태 반환
     * @return true: 정상, false: 실패
     */
    public boolean isHealthy() {
        return lastHealthStatus;
    }

    /**
     * 수동으로 Redis 연결 테스트
     * @return true: 연결 성공, false: 연결 실패
     */
    public boolean testConnection() {
        try {
            String pong = connectionFactory.getConnection().ping();
            boolean healthy = "PONG".equals(pong);
            log.info("[Redis Health Check] 수동 연결 테스트 결과: {}", healthy ? "성공" : "실패");
            return healthy;
        } catch (Exception e) {
            log.error("[Redis Health Check] 수동 연결 테스트 실패", e);
            return false;
        }
    }
}
