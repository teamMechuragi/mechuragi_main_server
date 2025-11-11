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
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tenMinutesLater = now.plusMinutes(10);
        LocalDateTime elevenMinutesLater = now.plusMinutes(11);

        List<VotePost> endingSoonVotes = votePostRepository.findVotesEndingInTenMinutes(
                tenMinutesLater, elevenMinutesLater
        );

        if (!endingSoonVotes.isEmpty()) {
            log.info("투표 종료 10분 전 알림 발송: {} 건", endingSoonVotes.size());

            endingSoonVotes.forEach(vote -> {
                votePostService.notifyVoteEndingSoon(vote.getId(), vote.getTitle());
            });
        }
    }

    /**
     * 만료된 투표 종료 처리 스케줄러
     * 매 1분마다 실행
     */
    @Scheduled(cron = "0 * * * * *") // 매 분 0초에 실행
    public void completeExpiredVotes() {
        LocalDateTime now = LocalDateTime.now();
        List<VotePost> expiredVotes = votePostRepository.findExpiredActiveVotes(now);

        if (!expiredVotes.isEmpty()) {
            log.info("만료된 투표 종료 처리: {} 건", expiredVotes.size());

            expiredVotes.forEach(vote -> {
                votePostService.completeVoteAndNotify(vote.getId());
            });
        }
    }
}
