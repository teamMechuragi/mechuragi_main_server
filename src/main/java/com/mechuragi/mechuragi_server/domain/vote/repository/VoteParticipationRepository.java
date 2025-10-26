package com.mechuragi.mechuragi_server.domain.vote.repository;

import com.mechuragi.mechuragi_server.domain.vote.entity.VoteParticipation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VoteParticipationRepository extends JpaRepository<VoteParticipation, Long> {

    // 사용자가 특정 투표에 참여했는지 확인
    Optional<VoteParticipation> findByMemberIdAndVotePostId(Long memberId, Long votePostId);

    // 특정 투표의 모든 참여 기록 조회
    List<VoteParticipation> findByVotePostId(Long votePostId);

    // 특정 선택지의 모든 참여 기록 조회
    List<VoteParticipation> findByVoteOptionId(Long voteOptionId);

    // 사용자별 투표 참여 기록 조회
    List<VoteParticipation> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}