package com.mechuragi.mechuragi_server.domain.preference.repository;

import com.mechuragi.mechuragi_server.domain.preference.entity.PreferenceFoodType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PreferenceFoodTypeRepository extends JpaRepository<PreferenceFoodType, Long> {

    List<PreferenceFoodType> findByPreferenceId(Long preferenceId);

    void deleteByPreferenceId(Long preferenceId);
}