package com.mechuragi.mechuragi_server.domain.preference.repository;

import com.mechuragi.mechuragi_server.domain.preference.entity.FoodPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FoodPreferenceRepository extends JpaRepository<FoodPreference, Long> {

    List<FoodPreference> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<FoodPreference> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT COUNT(fp) FROM FoodPreference fp WHERE fp.user.id = :userId")
    int countByUserId(@Param("userId") Long userId);

    void deleteByIdAndUserId(Long id, Long userId);
}