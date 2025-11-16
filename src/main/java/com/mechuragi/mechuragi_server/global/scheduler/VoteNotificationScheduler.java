package com.mechuragi.mechuragi_server.global.scheduler;

import com.mechuragi.mechuragi_server.domain.vote.entity.VotePost;
import com.mechuragi.mechuragi_server.domain.vote.repository.VotePostRepository;
import com.mechuragi.mechuragi_server.domain.vote.service.VotePostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class VoteNotificationScheduler {
    private final VotePostRepository votePostRepository;
    private final VotePostService votePostService;

    /**
     * 투표 종료 10분 전 알림 스케줄러
     * 매 1분마다 실행
     */
    @Scheduled(cron = "0 * * * * *") // 매 분 0초에 실행
    public void notifyVotesEndingSoon() {
        LocalDateTime now = LocalDateTime.now(); // KST 기준
        // 9분 30초 ~ 10분 30초 범위로 검색 (여유있게)
        LocalDateTime rangeStart = now.plusMinutes(9).plusSeconds(30);
        LocalDateTime rangeEnd = now.plusMinutes(10).plusSeconds(30);

        log.debug("[10분 전 알림 스케줄러] 실행: now={}, 검색범위={} ~ {}", now, rangeStart, rangeEnd);

        List<VotePost> endingSoonVotes = votePostRepository.findVotesEndingInTenMinutes(
                rangeStart, rangeEnd
        );

        log.debug("[10분 전 알림 스케줄러] 검색 결과: {} 건", endingSoonVotes.size());

        if (!endingSoonVotes.isEmpty()) {
            log.info("투표 종료 10분 전 알림 발송: {} 건", endingSoonVotes.size());

            endingSoonVotes.forEach(vote -> {
                try {
                    votePostService.notifyVoteEndingSoon(vote.getId(), vote.getTitle());
                } catch (Exception e) {
                    log.error("투표(ID: {}) 종료 10분 전 알림 발송 중 예외 발생. 다음 투표로 계속 진행. 에러 메시지: {}",
                            vote.getId(), e.getMessage(), e);
                }
            });
        }
    }

    /**
     * 만료된 투표 종료 처리 스케줄러
     * 매 1분마다 실행
     */
    @Scheduled(cron = "0 * * * * *") // 매 분 0초에 실행
    public void completeExpiredVotes() {
        LocalDateTime now = LocalDateTime.now(); // KST 기준

        log.debug("[투표 종료 스케줄러] 실행: now={}", now);

        List<VotePost> expiredVotes = votePostRepository.findExpiredActiveVotes(now);

        log.debug("[투표 종료 스케줄러] 검색 결과: {} 건", expiredVotes.size());

        if (!expiredVotes.isEmpty()) {
            log.info("만료된 투표 종료 처리: {} 건", expiredVotes.size());

            expiredVotes.forEach(vote -> {
                try {
                    // 개별 투표 처리 로직을 try-catch로 감싸서 예외 발생 시에도 다음 투표로 넘어갈 수 있도록 처리
                    votePostService.completeVoteAndNotify(vote.getId());
                } catch (Exception e) {
                    // 예외 발생 시 로그를 남기고, 현재 투표만 건너뛰고 다음 루프로 이동
                    log.error("투표(ID: {}) 종료 처리 중 예외 발생. 다음 투표로 계속 진행. 에러 메시지: {}",
                            vote.getId(), e.getMessage(), e);
                }
            });
        }
    }
}
