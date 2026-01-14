package com.mechuragi.mechuragi_server.domain.recommend.repository;

import com.mechuragi.mechuragi_server.domain.member.entity.Member;
import com.mechuragi.mechuragi_server.domain.recommend.entity.RecommendationSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecommendationSessionRepository extends JpaRepository<RecommendationSession, Long> {

    List<RecommendationSession> findByMemberOrderByCreatedAtDesc(Member member);
}
