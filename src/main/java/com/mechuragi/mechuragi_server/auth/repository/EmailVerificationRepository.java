package com.mechuragi.mechuragi_server.auth.repository;

import com.mechuragi.mechuragi_server.auth.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    /**
     * 회원 ID로 이메일 인증 정보 조회
     */
    Optional<EmailVerification> findByMemberId(Long memberId);

    /**
     * 회원 ID와 인증 코드로 조회
     */
    Optional<EmailVerification> findByMemberIdAndVerificationCode(Long memberId, String verificationCode);

    /**
     * 회원 ID로 이메일 인증 정보 삭제
     */
    void deleteByMemberId(Long memberId);
}
