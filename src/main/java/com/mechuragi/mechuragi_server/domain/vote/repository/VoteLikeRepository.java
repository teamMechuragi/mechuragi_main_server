package com.mechuragi.mechuragi_server.domain.vote.repository;

import com.mechuragi.mechuragi_server.domain.vote.entity.VoteLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VoteLikeRepository extends JpaRepository<VoteLike, Long> {

    // 사용자가 특정 투표에 좋아요를 눌렀는지 확인
    Optional<VoteLike> findByMemberIdAndVotePostId(Long memberId, Long votePostId);

    // 특정 투표의 좋아요 수 조회
    int countByVotePostId(Long votePostId);

    // 사용자가 좋아요 누른 투표 수 조회
    int countByMemberId(Long memberId);
}