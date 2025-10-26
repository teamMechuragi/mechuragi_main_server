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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public VoteParticipationResponseDTO participateVote(Long memberId, VoteParticipationRequestDTO request) {
        // 사용자 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 투표 조회
        VotePost votePost = votePostRepository.findById(request.voteId())
                .orElseThrow(() -> new BusinessException(ErrorCode.VOTE_NOT_FOUND));

        // 투표 상태 검증
        validateVoteStatus(votePost);

        // 기존 참여 확인 - 있으면 삭제 (다시 투표하기)
        List<VoteParticipation> existingParticipations =
                voteParticipationRepository.findByMemberIdAndVotePostId(memberId, request.voteId());

        if (!existingParticipations.isEmpty()) {
            voteParticipationRepository.deleteByMemberIdAndVotePostId(memberId, request.voteId());
            log.info("Member {} re-voting, deleted existing participations for vote {}",
                    memberId, request.voteId());
        }

        // 복수 선택 검증
        if (!votePost.getAllowMultipleChoice() && request.optionIds().size() > 1) {
            throw new BusinessException(ErrorCode.MULTIPLE_CHOICE_NOT_ALLOWED);
        }

        // 옵션 조회 및 검증
        List<VoteOption> voteOptions = new ArrayList<>();
        for (Long optionId : request.optionIds()) {
            VoteOption voteOption = voteOptionRepository.findById(optionId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.VOTE_OPTION_NOT_FOUND));

            // 투표 옵션이 해당 투표에 속하는지 검증
            if (!voteOption.getVotePost().getId().equals(request.voteId())) {
                throw new BusinessException(ErrorCode.VOTE_OPTION_NOT_FOUND);
            }

            voteOptions.add(voteOption);
        }

        // 참여 기록 생성
        List<VoteParticipation> participations = new ArrayList<>();
        for (VoteOption voteOption : voteOptions) {
            VoteParticipation participation = VoteParticipation.builder()
                    .member(member)
                    .votePost(votePost)
                    .voteOption(voteOption)
                    .build();
            participations.add(voteParticipationRepository.save(participation));
        }

        log.info("Member {} participated in vote {} with options {}",
                memberId, request.voteId(), request.optionIds());

        return VoteParticipationResponseDTO.from(
                votePost.getId(),
                votePost.getTitle(),
                participations
        );
    }

    @Transactional
    public void cancelParticipation(Long memberId, Long voteId) {
        // 투표 조회
        VotePost votePost = votePostRepository.findById(voteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOTE_NOT_FOUND));

        // 투표 상태 검증
        validateVoteStatus(votePost);

        // 참여 기록 조회
        List<VoteParticipation> participations =
                voteParticipationRepository.findByMemberIdAndVotePostId(memberId, voteId);

        if (participations.isEmpty()) {
            throw new BusinessException(ErrorCode.VOTE_PARTICIPATION_NOT_FOUND);
        }

        // 참여 기록 삭제
        voteParticipationRepository.deleteByMemberIdAndVotePostId(memberId, voteId);

        log.info("Member {} cancelled participation in vote {}", memberId, voteId);
    }

    public VoteParticipationResponseDTO getMyParticipation(Long memberId, Long voteId) {
        // 투표 조회
        VotePost votePost = votePostRepository.findById(voteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOTE_NOT_FOUND));

        // 참여 기록 조회
        List<VoteParticipation> participations =
                voteParticipationRepository.findByMemberIdAndVotePostId(memberId, voteId);

        if (participations.isEmpty()) {
            throw new BusinessException(ErrorCode.VOTE_PARTICIPATION_NOT_FOUND);
        }

        return VoteParticipationResponseDTO.from(
                votePost.getId(),
                votePost.getTitle(),
                participations
        );
    }

    public boolean hasParticipated(Long memberId, Long voteId) {
        List<VoteParticipation> participations =
                voteParticipationRepository.findByMemberIdAndVotePostId(memberId, voteId);
        return !participations.isEmpty();
    }

    private void validateVoteStatus(VotePost votePost) {
        // 완료된 투표인지 확인
        if (votePost.getStatus() == VotePost.VoteStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.VOTE_ALREADY_COMPLETED);
        }

        // 마감 시간이 지났는지 확인
        if (votePost.isExpired()) {
            throw new BusinessException(ErrorCode.VOTE_EXPIRED);
        }
    }
}
