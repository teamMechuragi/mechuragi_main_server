package com.mechuragi.mechuragi_server.domain.vote.repository;

import com.mechuragi.mechuragi_server.domain.vote.entity.VotePost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VotePostRepository extends JpaRepository<VotePost, Long> {
    // 작성자와 ID로 투표 게시물 조회 (권한 확인용)
    Optional<VotePost> findByIdAndAuthorId(Long id, Long authorId);

    // 진행 중인 투표 목록 조회 (최신순)
    @Query("SELECT v FROM VotePost v WHERE v.status = 'ACTIVE' AND v.deadline > :now ORDER BY v.createdAt DESC")
    Page<VotePost> findActiveVotes(@Param("now") LocalDateTime now, Pageable pageable);

    // 완료된 투표 목록 조회 (최신순)
    @Query("SELECT v FROM VotePost v WHERE v.status = 'COMPLETED' OR v.deadline <= :now ORDER BY v.createdAt DESC")
    Page<VotePost> findCompletedVotes(@Param("now") LocalDateTime now, Pageable pageable);

    // 사용자별 투표 게시물 조회
    Page<VotePost> findByAuthorIdOrderByCreatedAtDesc(Long authorId, Pageable pageable);

    // 마감 시간이 지난 ACTIVE 상태 투표들 조회 (배치 처리용)
    @Query("SELECT v FROM VotePost v WHERE v.status = 'ACTIVE' AND v.deadline <= :now")
    List<VotePost> findExpiredActiveVotes(@Param("now") LocalDateTime now);
}