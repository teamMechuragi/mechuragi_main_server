package com.mechuragi.mechuragi_server.auth.service;

import com.mechuragi.mechuragi_server.auth.dto.TokenResponse;
import com.mechuragi.mechuragi_server.auth.entity.RefreshToken;
import com.mechuragi.mechuragi_server.auth.repository.RefreshTokenRepository;
import com.mechuragi.mechuragi_server.auth.util.JwtTokenProvider;
import com.mechuragi.mechuragi_server.domain.member.entity.Member;
import com.mechuragi.mechuragi_server.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JwtService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberRepository memberRepository;

    /**
     * Access Token과 Refresh Token을 발급하고 Refresh Token을 DB에 저장
     */
    @Transactional
    public TokenResponse issueTokens(Member member) {
        // Access Token 생성
        String accessToken = jwtTokenProvider.generateAccessToken(
                member.getEmail(),
                member.getId(),
                member.getRole().name()
        );

        // Refresh Token 생성
        String refreshTokenValue = jwtTokenProvider.generateRefreshToken(member.getEmail());
        Date expiryDate = jwtTokenProvider.getExpirationFromToken(refreshTokenValue);

        // Refresh Token 저장 또는 업데이트
        RefreshToken refreshToken = refreshTokenRepository.findByMemberId(member.getId())
                .map(token -> {
                    token.updateToken(refreshTokenValue, expiryDate);
                    return token;
                })
                .orElseGet(() -> {
                    RefreshToken newToken = RefreshToken.builder()
                            .memberId(member.getId())
                            .token(refreshTokenValue)
                            .expiryDate(expiryDate)
                            .build();
                    return refreshTokenRepository.save(newToken);
                });

        log.info("토큰 발급 완료: memberId={}", member.getId());

        return TokenResponse.of(accessToken, refreshToken.getToken(), jwtTokenProvider.getAccessTokenExpiration());
    }

    /**
     * Refresh Token으로 Access Token 재발급
     */
    @Transactional
    public TokenResponse refreshAccessToken(String refreshTokenValue) {
        // Refresh Token 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshTokenValue)) {
            throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다.");
        }

        // DB에서 Refresh Token 조회
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Refresh Token을 찾을 수 없습니다."));

        // 만료 여부 확인
        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new IllegalArgumentException("만료된 Refresh Token입니다.");
        }

        // 회원 조회
        Member member = memberRepository.findById(refreshToken.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        // 새로운 Access Token 발급
        String newAccessToken = jwtTokenProvider.generateAccessToken(
                member.getEmail(),
                member.getId(),
                member.getRole().name()
        );

        log.info("Access Token 재발급 완료: memberId={}", member.getId());

        return TokenResponse.of(newAccessToken, refreshTokenValue, jwtTokenProvider.getAccessTokenExpiration());
    }

    /**
     * 로그아웃 - Refresh Token 삭제
     */
    @Transactional
    public void logout(Long memberId) {
        refreshTokenRepository.deleteByMemberId(memberId);
        log.info("로그아웃 완료: memberId={}", memberId);
    }
}
