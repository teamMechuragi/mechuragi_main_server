package com.mechuragi.mechuragi_server.domain.ai.repository;

import com.mechuragi.mechuragi_server.domain.ai.entity.ScrapedFood;
import com.mechuragi.mechuragi_server.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScrapedFoodRepository extends JpaRepository<ScrapedFood, Long> {

    List<ScrapedFood> findByMemberOrderByCreatedAtDesc(Member member);

    List<ScrapedFood> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}
