package com.mechuragi.mechuragi_server.domain.recommend.repository;

import com.mechuragi.mechuragi_server.domain.recommend.entity.RecommendedFood;
import com.mechuragi.mechuragi_server.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecommendedFoodRepository extends JpaRepository<RecommendedFood, Long> {

    List<RecommendedFood> findByMemberOrderByCreatedAtDesc(Member member);
}
