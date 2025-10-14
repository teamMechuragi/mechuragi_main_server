package com.mechuragi.mechuragi_server.domain.preference.repository;

import com.mechuragi.mechuragi_server.domain.preference.entity.DislikedFood;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DislikedFoodRepository extends JpaRepository<DislikedFood, Long> {

    List<DislikedFood> findByPreferenceId(Long preferenceId);

    void deleteByPreferenceId(Long preferenceId);
}