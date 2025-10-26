package com.mechuragi.mechuragi_server.domain.vote.repository;

import com.mechuragi.mechuragi_server.domain.vote.entity.VoteComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VoteCommentRepository extends JpaRepository<VoteComment, Long> {

    // 투표 게시물별 댓글 조회 (최신순)
    Page<VoteComment> findByVotePostIdOrderByCreatedAtDesc(Long votePostId, Pageable pageable);

    // 작성자와 ID로 댓글 조회 (권한 확인용)
    Optional<VoteComment> findByIdAndAuthorId(Long id, Long authorId);

    // 투표 게시물의 댓글 수 조회
    int countByVotePostId(Long votePostId);
}