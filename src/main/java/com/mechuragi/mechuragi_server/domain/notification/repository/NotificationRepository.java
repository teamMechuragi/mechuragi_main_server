package com.mechuragi.mechuragi_server.domain.notification.repository;

import com.mechuragi.mechuragi_server.domain.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 특정 회원의 알림 목록 조회 (페이징)
     */
    Page<Notification> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    /**
     * 특정 회원의 안 읽은 알림 개수 조회
     */
    long countByMemberIdAndIsReadFalse(Long memberId);

    /**
     * 특정 회원의 특정 투표에 대한 특정 타입의 알림이 이미 존재하는지 확인
     * (중복 발송 방지용)
     */
    @Query("SELECT COUNT(n) > 0 FROM Notification n " +
           "WHERE n.member.id = :memberId " +
           "AND n.voteId = :voteId " +
           "AND n.type = :type")
    boolean existsByMemberIdAndVoteIdAndType(
            @Param("memberId") Long memberId,
            @Param("voteId") Long voteId,
            @Param("type") com.mechuragi.mechuragi_server.domain.notification.dto.VoteNotificationType type
    );
}
