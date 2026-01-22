package com.mechuragi.mechuragi_server.global.scheduler;

import com.mechuragi.mechuragi_server.domain.vote.entity.VotePost;
import com.mechuragi.mechuragi_server.domain.vote.repository.VotePostRepository;
import com.mechuragi.mechuragi_server.domain.vote.service.VotePostService;
import com.mechuragi.mechuragi_server.global.redis.health.RedisHealthChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 투표 알림 폴백 스케줄러
 *
 * Redis Keyspace Notifications 기반 이벤트 시스템이 정상 동작하지 않을 때를 대비한 폴백 스케줄러.
 * Redis 연결이 비정상일 때만 DB 폴링을 통해 알림을 처리합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VoteNotificationFallbackScheduler {

    private final VotePostRepository votePostRepository;
    private final VotePostService votePostService;
    private final RedisHealthChecker redisHealthChecker;

    /**
     * 투표 종료 10분 전 알림 폴백 스케줄러 (5분마다 실행)
     * Redis 비정상 시에만 동작
     */
    @Scheduled(cron = "0 */5 * * * *")
    @SchedulerLock(name = "fallbackNotifyVotesEndingSoon", lockAtMostFor = "4m", lockAtLeastFor = "1m")
    public void fallbackNotifyVotesEndingSoon() {
        if (redisHealthChecker.isHealthy()) {
            log.debug("[Fallback 10분 전 알림] Redis 정상 - 스킵");
            return;
        }

        log.warn("[Fallback 10분 전 알림] Redis 비정상 - DB 폴링 실행");

        Instant now = Instant.now();
        // 5분 주기이므로 범위를 넓게 설정 (4분 30초 ~ 10분 30초)
        Instant rangeStart = now.plus(4, ChronoUnit.MINUTES).plus(30, ChronoUnit.SECONDS);
        Instant rangeEnd = now.plus(10, ChronoUnit.MINUTES).plus(30, ChronoUnit.SECONDS);

        log.debug("[Fallback 10분 전 알림] 검색범위: {} ~ {}", rangeStart, rangeEnd);

        List<VotePost> endingSoonVotes =
                votePostRepository.findVotesEndingInTenMinutes(rangeStart, rangeEnd);

        log.debug("[Fallback 10분 전 알림] 검색 결과: {} 건", endingSoonVotes.size());

        if (!endingSoonVotes.isEmpty()) {
            log.info("[Fallback] 투표 종료 10분 전 알림 발송: {} 건", endingSoonVotes.size());
            endingSoonVotes.forEach(vote -> {
                try {
                    votePostService.notifyVoteEndingSoon(vote.getId(), vote.getTitle());
                } catch (Exception e) {
                    log.error("[Fallback] 투표(ID: {}) 10분 전 알림 발송 실패: {}",
                            vote.getId(), e.getMessage(), e);
                }
            });
        }
    }

    /**
     * 만료된 투표 종료 처리 폴백 스케줄러 (5분마다 실행)
     * Redis 비정상 시에만 동작
     */
    @Scheduled(cron = "0 */5 * * * *")
    @SchedulerLock(name = "fallbackCompleteExpiredVotes", lockAtMostFor = "4m", lockAtLeastFor = "1m")
    public void fallbackCompleteExpiredVotes() {
        if (redisHealthChecker.isHealthy()) {
            log.debug("[Fallback 투표 종료] Redis 정상 - 스킵");
            return;
        }

        log.warn("[Fallback 투표 종료] Redis 비정상 - DB 폴링 실행");

        Instant now = Instant.now();

        log.debug("[Fallback 투표 종료] 실행: now={}", now);

        List<VotePost> expiredVotes = votePostRepository.findExpiredActiveVotes(now);

        log.debug("[Fallback 투표 종료] 검색 결과: {} 건", expiredVotes.size());

        if (!expiredVotes.isEmpty()) {
            log.info("[Fallback] 만료된 투표 종료 처리: {} 건", expiredVotes.size());

            expiredVotes.forEach(vote -> {
                try {
                    votePostService.completeVoteAndNotify(vote.getId());
                } catch (Exception e) {
                    log.error("[Fallback] 투표(ID: {}) 종료 처리 실패: {}",
                            vote.getId(), e.getMessage(), e);
                }
            });
        }
    }
}
