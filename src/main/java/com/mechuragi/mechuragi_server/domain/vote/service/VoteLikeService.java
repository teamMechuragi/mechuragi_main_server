package com.mechuragi.mechuragi_server.domain.vote.service;

import com.mechuragi.mechuragi_server.domain.member.entity.Member;
import com.mechuragi.mechuragi_server.domain.member.repository.MemberRepository;
import com.mechuragi.mechuragi_server.domain.vote.entity.VoteLike;
import com.mechuragi.mechuragi_server.domain.vote.entity.VotePost;
import com.mechuragi.mechuragi_server.domain.vote.repository.VoteLikeRepository;
import com.mechuragi.mechuragi_server.domain.vote.repository.VotePostRepository;
import com.mechuragi.mechuragi_server.global.exception.BusinessException;
import com.mechuragi.mechuragi_server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteLikeService {

    private final VoteLikeRepository voteLikeRepository;
    private final VotePostRepository votePostRepository;
    private final MemberRepository memberRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String HOT_VOTE_SORTED_SET_KEY = "vote:hot";

    @Transactional
    public boolean toggleLike(Long memberId, Long voteId) {
        // 사용자 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 투표 조회
        VotePost votePost = votePostRepository.findById(voteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOTE_NOT_FOUND));

        // 이미 좋아요를 눌렀는지 확인
        var existingLike = voteLikeRepository.findByMemberIdAndVotePostId(memberId, voteId);

        if (existingLike.isPresent()) {
            // 좋아요가 있으면 취소
            voteLikeRepository.delete(existingLike.get());
            decrementLikeCount(voteId);
            updateHotVoteScore(voteId);
            log.info("Vote unliked: voteId={}, memberId={}, Redis 실시간 반영 완료", voteId, memberId);
            return false;
        } else {
            // 좋아요가 없으면 생성
            VoteLike voteLike = VoteLike.builder()
                    .member(member)
                    .votePost(votePost)
                    .build();
            voteLikeRepository.save(voteLike);
            incrementLikeCount(voteId);
            updateHotVoteScore(voteId);
            log.info("Vote liked: voteId={}, memberId={}, Redis 실시간 반영 완료", voteId, memberId);
            return true;
        }
    }

    public boolean isLiked(Long memberId, Long voteId) {
        return voteLikeRepository.findByMemberIdAndVotePostId(memberId, voteId).isPresent();
    }

    public int getLikeCount(Long voteId) {
        return voteLikeRepository.countByVotePostId(voteId);
    }

    // helper
    private void incrementLikeCount(Long voteId) {
        String key = "vote:" + voteId + ":likes";
        redisTemplate.opsForValue().increment(key);
    }

    private void decrementLikeCount(Long voteId) {
        String key = "vote:" + voteId + ":likes";
        redisTemplate.opsForValue().decrement(key);
    }

    private void updateHotVoteScore(Long voteId) {
        String participantsKey = "vote:" + voteId + ":participants";
        String likesKey = "vote:" + voteId + ":likes";

        String participantsStr = redisTemplate.opsForValue().get(participantsKey);
        String likesStr = redisTemplate.opsForValue().get(likesKey);

        int participants = participantsStr != null ? Integer.parseInt(participantsStr) : 0;
        int likes = likesStr != null ? Integer.parseInt(likesStr) : 0;

        double score = participants + likes * 0.5;

        // 마감 시간 가중치 추가 (48시간 이내면 보너스)
        VotePost votePost = votePostRepository.findById(voteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOTE_NOT_FOUND));
        long hoursRemaining = Duration.between(LocalDateTime.now(), votePost.getDeadline()).toHours();
        if (hoursRemaining > 0 && hoursRemaining <= 48) {
            double deadlineBonus = (48 - hoursRemaining) / 20.0;
            score += deadlineBonus;
        }

        redisTemplate.opsForZSet().add(HOT_VOTE_SORTED_SET_KEY, voteId.toString(), score);
    }
}