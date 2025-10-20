package com.mechuragi.mechuragi_server.auth.repository;

import com.mechuragi.mechuragi_server.auth.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    /**
     * 이메일로 인증 정보 조회
     */
    Optional<EmailVerification> findByEmail(String email);

    /**
     * 이메일과 인증 코드로 조회
     */
    Optional<EmailVerification> findByEmailAndVerificationCode(String email, String verificationCode);

    /**
     * 이메일로 인증 정보 삭제
     */
    void deleteByEmail(String email);
}
