package com.mechuragi.mechuragi_server.domain.preference.repository;

import com.mechuragi.mechuragi_server.domain.preference.entity.PreferenceTaste;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PreferenceTasteRepository extends JpaRepository<PreferenceTaste, Long> {

    List<PreferenceTaste> findByPreferenceId(Long preferenceId);

    void deleteByPreferenceId(Long preferenceId);
}