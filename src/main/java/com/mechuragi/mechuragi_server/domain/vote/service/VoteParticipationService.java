package com.mechuragi.mechuragi_server.domain.vote.service;

import com.mechuragi.mechuragi_server.domain.member.entity.Member;
import com.mechuragi.mechuragi_server.domain.member.repository.MemberRepository;
import com.mechuragi.mechuragi_server.domain.vote.dto.VoteParticipationRequestDTO;
import com.mechuragi.mechuragi_server.domain.vote.dto.VoteParticipationResponseDTO;
import com.mechuragi.mechuragi_server.domain.vote.entity.VoteOption;
import com.mechuragi.mechuragi_server.domain.vote.entity.VoteParticipation;
import com.mechuragi.mechuragi_server.domain.vote.entity.VotePost;
import com.mechuragi.mechuragi_server.domain.vote.repository.VoteOptionRepository;
import com.mechuragi.mechuragi_server.domain.vote.repository.VoteParticipationRepository;
import com.mechuragi.mechuragi_server.domain.vote.repository.VotePostRepository;
import com.mechuragi.mechuragi_server.global.exception.BusinessException;
import com.mechuragi.mechuragi_server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteParticipationService {

    private final VoteParticipationRepository voteParticipationRepository;
    private final VotePostRepository votePostRepository;
    private final VoteOptionRepository voteOptionRepository;
    private final MemberRepository memberRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String HOT_VOTE_SORTED_SET_KEY = "vote:hot";

    @Transactional
    public VoteParticipationResponseDTO participateVote(Long memberId, VoteParticipationRequestDTO request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        VotePost votePost = votePostRepository.findById(request.getVoteId())
                .orElseThrow(() -> new BusinessException(ErrorCode.VOTE_NOT_FOUND));

        validateVoteStatus(votePost);

        // 기존 참여 삭제 (재투표)
        List<VoteParticipation> existingParticipations =
                voteParticipationRepository.findByMemberIdAndVotePostId(memberId, request.getVoteId());

        if (!existingParticipations.isEmpty()) {
            voteParticipationRepository.deleteByMemberIdAndVotePostId(memberId, request.getVoteId());
            // Redis에서도 기존 투표 수 반영 제거
            existingParticipations.forEach(p -> decrementOptionCount(votePost.getId(), p.getVoteOption().getId()));
            decrementParticipantCount(votePost.getId());
            log.info("사용자 {}가 재투표하여 기존 참여 기록 삭제, Redis 값 갱신 완료: 투표 {}", memberId, request.getVoteId());
        }

        if (!votePost.getAllowMultipleChoice() && request.getOptionIds().size() > 1) {
            throw new BusinessException(ErrorCode.MULTIPLE_CHOICE_NOT_ALLOWED);
        }

        List<VoteOption> voteOptions = new ArrayList<>();
        for (Long optionId : request.getOptionIds()) {
            VoteOption voteOption = voteOptionRepository.findById(optionId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.VOTE_OPTION_NOT_FOUND));

            if (!voteOption.getVotePost().getId().equals(request.getVoteId())) {
                throw new BusinessException(ErrorCode.VOTE_OPTION_NOT_FOUND);
            }

            voteOptions.add(voteOption);
        }

        List<VoteParticipation> participations = new ArrayList<>();
        for (VoteOption voteOption : voteOptions) {
            VoteParticipation participation = VoteParticipation.builder()
                    .member(member)
                    .votePost(votePost)
                    .voteOption(voteOption)
                    .build();
            participations.add(voteParticipationRepository.save(participation));
            incrementOptionCount(votePost.getId(), voteOption.getId());
        }

        incrementParticipantCount(votePost.getId());
        updateHotVoteScore(votePost.getId());

        log.info("사용자 {}가 투표 {}에 참여, 선택 옵션 {} Redis 실시간 반영 완료",
                memberId, request.getVoteId(), request.getOptionIds());

        return VoteParticipationResponseDTO.from(votePost.getId(), votePost.getTitle(), participations);
    }

    @Transactional
    public void cancelParticipation(Long memberId, Long voteId) {
        VotePost votePost = votePostRepository.findById(voteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOTE_NOT_FOUND));

        validateVoteStatus(votePost);

        List<VoteParticipation> participations =
                voteParticipationRepository.findByMemberIdAndVotePostId(memberId, voteId);

        if (participations.isEmpty()) {
            throw new BusinessException(ErrorCode.VOTE_PARTICIPATION_NOT_FOUND);
        }

        voteParticipationRepository.deleteByMemberIdAndVotePostId(memberId, voteId);

        participations.forEach(p -> decrementOptionCount(votePost.getId(), p.getVoteOption().getId()));
        decrementParticipantCount(votePost.getId());
        updateHotVoteScore(votePost.getId());

        log.info("사용자 {}가 투표 {} 참여 취소, Redis 실시간 반영 완료", memberId, voteId);
    }

    public VoteParticipationResponseDTO getMyParticipation(Long memberId, Long voteId) {
        VotePost votePost = votePostRepository.findById(voteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOTE_NOT_FOUND));

        List<VoteParticipation> participations =
                voteParticipationRepository.findByMemberIdAndVotePostId(memberId, voteId);

        // 투표 참여하지 않은 경우 빈 응답 반환 (에러 아님)
        if (participations.isEmpty()) {
            return VoteParticipationResponseDTO.builder()
                    .voteId(votePost.getId())
                    .voteTitle(votePost.getTitle())
                    .participatedOptions(List.of())
                    .participatedAt(null)
                    .build();
        }

        return VoteParticipationResponseDTO.from(votePost.getId(), votePost.getTitle(), participations);
    }

    public boolean hasParticipated(Long memberId, Long voteId) {
        List<VoteParticipation> participations =
                voteParticipationRepository.findByMemberIdAndVotePostId(memberId, voteId);
        return !participations.isEmpty();
    }

    private void validateVoteStatus(VotePost votePost) {
        if (votePost.getStatus() == VotePost.VoteStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.VOTE_ALREADY_COMPLETED);
        }
        if (votePost.isExpired()) {
            throw new BusinessException(ErrorCode.VOTE_EXPIRED);
        }
    }

    // helper
    private void incrementParticipantCount(Long voteId) {
        String key = "vote:" + voteId + ":participants";
        redisTemplate.opsForValue().increment(key);
    }

    private void decrementParticipantCount(Long voteId) {
        String key = "vote:" + voteId + ":participants";
        redisTemplate.opsForValue().decrement(key);
    }

    private void incrementOptionCount(Long voteId, Long optionId) {
        String key = "vote:" + voteId + ":option:" + optionId + ":count";
        redisTemplate.opsForValue().increment(key);
    }

    private void decrementOptionCount(Long voteId, Long optionId) {
        String key = "vote:" + voteId + ":option:" + optionId + ":count";
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

        long hoursRemaining = Duration.between(Instant.now(), votePost.getDeadline()).toHours();
        if (hoursRemaining > 0 && hoursRemaining <= 48) {
            double deadlineBonus = (48 - hoursRemaining) / 20.0;
            score += deadlineBonus;
        }

        redisTemplate.opsForZSet().add(HOT_VOTE_SORTED_SET_KEY, voteId.toString(), score);
    }
}