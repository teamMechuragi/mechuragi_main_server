package com.mechuragi.mechuragi_server.global.redis.publisher;

import com.mechuragi.mechuragi_server.domain.vote.dto.PopularMenuResponseDTO;
import com.mechuragi.mechuragi_server.domain.vote.entity.VoteOption;
import com.mechuragi.mechuragi_server.domain.vote.entity.VotePost;
import com.mechuragi.mechuragi_server.domain.vote.repository.VoteLikeRepository;
import com.mechuragi.mechuragi_server.domain.vote.repository.VoteParticipationRepository;
import com.mechuragi.mechuragi_server.domain.vote.repository.VotePostRepository;
import com.mechuragi.mechuragi_server.domain.vote.service.PopularMenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisDataInitializer {

    private final VotePostRepository votePostRepository;
    private final VoteParticipationRepository voteParticipationRepository;
    private final VoteLikeRepository voteLikeRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final PopularMenuService popularMenuService;

    private static final String HOT_VOTE_SORTED_SET_KEY = "vote:hot";

    @Transactional(readOnly = true)
    @EventListener(ApplicationReadyEvent.class)
    public void initializeRedisData() {
        log.info("Redis 데이터 초기화 시작");

        List<VotePost> allVotes = votePostRepository.findAll();

        for (VotePost vote : allVotes) {
            Long voteId = vote.getId();

            // 1. 참여자 수 동기화
            long participantCount = voteParticipationRepository.countDistinctMemberByVotePostId(voteId);
            String participantsKey = "vote:" + voteId + ":participants";
            redisTemplate.opsForValue().set(participantsKey, String.valueOf(participantCount));

            // 2. 좋아요 수 동기화
            int likeCount = voteLikeRepository.countByVotePostId(voteId);
            String likesKey = "vote:" + voteId + ":likes";
            redisTemplate.opsForValue().set(likesKey, String.valueOf(likeCount));

            // 3. 각 옵션별 투표수 동기화
            for (VoteOption option : vote.getVoteOptions()) {
                int optionVoteCount = voteParticipationRepository.countByVoteOptionId(option.getId());
                String optionKey = "vote:" + voteId + ":option:" + option.getId() + ":count";
                redisTemplate.opsForValue().set(optionKey, String.valueOf(optionVoteCount));
            }

            // 4. 핫한 투표 점수 계산 (참여자 + 좋아요*0.5 + 마감임박보너스)
            double score = participantCount + likeCount * 0.5;

            // 마감 시간 가중치 추가 (48시간 이내면 보너스)
            long hoursRemaining = Duration.between(Instant.now(), vote.getDeadline()).toHours();
            if (hoursRemaining > 0 && hoursRemaining <= 48) {
                double deadlineBonus = (48 - hoursRemaining) / 20.0;
                score += deadlineBonus;
            }

            redisTemplate.opsForZSet().add(HOT_VOTE_SORTED_SET_KEY, voteId.toString(), score);
        }

        log.info("Redis 데이터 초기화 완료: 총 {}개 투표 동기화", allVotes.size());

        // 5. 실시간 인기 메뉴 캐시 초기화 (warm-up)
        try {
            Long hotVoteCount = redisTemplate.opsForZSet().zCard(HOT_VOTE_SORTED_SET_KEY);
            List<PopularMenuResponseDTO> popularMenus = popularMenuService.getPopularMenus();

            // PopularMenuService 내부에서 이미 상세 로그 출력
            log.info("인기 메뉴 Top 10 초기화 완료: Hot 투표 {}개, 총 Top {}개 메뉴 생성됨",
                    hotVoteCount != null ? hotVoteCount : 0, popularMenus.size());
        } catch (Exception e) {
            log.warn("인기 메뉴 초기화 실패: {}", e.getMessage());
        }
    }
}
