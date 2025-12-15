package com.mechuragi.mechuragi_server.global.scheduler;

import com.mechuragi.mechuragi_server.domain.vote.entity.VotePost;
import com.mechuragi.mechuragi_server.domain.vote.repository.VotePostRepository;
import com.mechuragi.mechuragi_server.domain.vote.service.VotePostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class VoteNotificationScheduler {

    private final VotePostRepository votePostRepository;
    private final VotePostService votePostService;

    /**
     * 투표 종료 10분 전 알림 스케줄러
     */
    @Scheduled(cron = "0 * * * * *")
    @SchedulerLock(name = "notifyVotesEndingSoon", lockAtMostFor = "58s", lockAtLeastFor = "30s")
    public void notifyVotesEndingSoon() {

        Instant now = Instant.now();
        Instant rangeStart = now.plus(9, ChronoUnit.MINUTES).plus(30, ChronoUnit.SECONDS);
        Instant rangeEnd = now.plus(10, ChronoUnit.MINUTES).plus(30, ChronoUnit.SECONDS);

        log.debug("[10분 전 알림 스케줄러] 실행: now={}, 검색범위={} ~ {}", now, rangeStart, rangeEnd);

        List<VotePost> endingSoonVotes =
                votePostRepository.findVotesEndingInTenMinutes(rangeStart, rangeEnd);

        log.debug("[10분 전 알림 스케줄러] 검색 결과: {} 건", endingSoonVotes.size());

        if (!endingSoonVotes.isEmpty()) {
            log.info("투표 종료 10분 전 알림 발송: {} 건", endingSoonVotes.size());
            endingSoonVotes.forEach(vote -> {
                try {
                    votePostService.notifyVoteEndingSoon(vote.getId(), vote.getTitle());
                } catch (Exception e) {
                    log.error("투표(ID: {}) 종료 10분 전 알림 발송 중 예외 발생. 다음 투표로 계속 진행. 에러: {}",
                            vote.getId(), e.getMessage(), e);
                }
            });
        }
    }

    /**
     * 만료된 투표 종료 처리 스케줄러
     */
    @Scheduled(cron = "0 * * * * *")
    @SchedulerLock(name = "completeExpiredVotes", lockAtMostFor = "58s", lockAtLeastFor = "30s")
    public void completeExpiredVotes() {

        Instant now = Instant.now();

        log.debug("[투표 종료 스케줄러] 실행: now={}", now);

        List<VotePost> expiredVotes = votePostRepository.findExpiredActiveVotes(now);

        log.debug("[투표 종료 스케줄러] 검색 결과: {} 건", expiredVotes.size());

        if (!expiredVotes.isEmpty()) {
            log.info("만료된 투표 종료 처리: {} 건", expiredVotes.size());

            expiredVotes.forEach(vote -> {
                try {
                    votePostService.completeVoteAndNotify(vote.getId());
                } catch (Exception e) {
                    log.error("투표(ID: {}) 종료 처리 중 예외 발생. 다음 투표로 계속 진행. 에러 메시지: {}",
                            vote.getId(), e.getMessage(), e);
                }
            });
        }
    }
}
