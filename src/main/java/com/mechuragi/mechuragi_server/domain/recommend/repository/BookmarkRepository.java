package com.mechuragi.mechuragi_server.domain.recommend.repository;

import com.mechuragi.mechuragi_server.domain.recommend.entity.Bookmark;
import com.mechuragi.mechuragi_server.domain.recommend.entity.RecommendationSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    Optional<Bookmark> findByMemberIdAndSessionId(Long memberId, Long sessionId);

    boolean existsByMemberIdAndSessionId(Long memberId, Long sessionId);

    @Query("SELECT DISTINCT s FROM Bookmark b " +
           "JOIN b.session s " +
           "JOIN FETCH s.recommendedFoods " +
           "WHERE b.member.id = :memberId " +
           "ORDER BY s.createdAt DESC")
    List<RecommendationSession> findBookmarkedSessionsByMemberId(@Param("memberId") Long memberId);

    @Query("SELECT b.session.id FROM Bookmark b WHERE b.member.id = :memberId")
    Set<Long> findSessionIdsByMemberId(@Param("memberId") Long memberId);
}
