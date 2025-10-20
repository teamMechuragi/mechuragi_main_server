package com.mechuragi.mechuragi_server.domain.preference.repository;

import com.mechuragi.mechuragi_server.domain.preference.entity.FoodPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FoodPreferenceRepository extends JpaRepository<FoodPreference, Long> {

    List<FoodPreference> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    Optional<FoodPreference> findByIdAndMemberId(Long id, Long memberId);

    @Query("SELECT COUNT(fp) FROM FoodPreference fp WHERE fp.member.id = :memberId")
    int countByMemberId(@Param("memberId") Long memberId);

    void deleteByIdAndMemberId(Long id, Long memberId);
}