package com.mechuragi.mechuragi_server.domain.vote.repository;

import com.mechuragi.mechuragi_server.domain.vote.entity.VoteOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VoteOptionRepository extends JpaRepository<VoteOption, Long> {

    // 투표 게시물별 선택지 조회 (표시 순서대로)
    List<VoteOption> findByVotePostIdOrderByDisplayOrder(Long votePostId);

    // 투표 게시물의 모든 선택지 삭제
    void deleteByVotePostId(Long votePostId);
}