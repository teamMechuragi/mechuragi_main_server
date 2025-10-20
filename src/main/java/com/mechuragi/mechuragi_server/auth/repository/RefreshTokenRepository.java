package com.mechuragi.mechuragi_server.auth.repository;

import com.mechuragi.mechuragi_server.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByMemberId(Long memberId);

    Optional<RefreshToken> findByToken(String token);

    void deleteByMemberId(Long memberId);
}
