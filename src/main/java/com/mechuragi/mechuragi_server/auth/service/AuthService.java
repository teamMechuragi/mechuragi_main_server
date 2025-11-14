package com.mechuragi.mechuragi_server.auth.service;

import com.mechuragi.mechuragi_server.auth.dto.LoginRequest;
import com.mechuragi.mechuragi_server.auth.dto.LoginResponse;
import com.mechuragi.mechuragi_server.auth.dto.TokenResponse;
import com.mechuragi.mechuragi_server.domain.member.dto.MemberResponse;
import com.mechuragi.mechuragi_server.domain.member.entity.Member;
import com.mechuragi.mechuragi_server.domain.member.entity.type.AuthProvider;
import com.mechuragi.mechuragi_server.domain.member.entity.type.MemberStatus;
import com.mechuragi.mechuragi_server.domain.member.repository.MemberRepository;
import com.mechuragi.mechuragi_server.domain.member.service.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MemberMapper memberMapper;

    /**
     * 일반 로그인
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        // 회원 조회
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 일치하지 않습니다."));

        // 소셜 로그인 회원인 경우
        if (member.getProvider() != AuthProvider.NORMAL) {
            throw new IllegalArgumentException("소셜 로그인으로 가입된 계정입니다.");
        }

        // 비밀번호 확인
        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 일치하지 않습니다.");
        }

        // 계정 상태 확인
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new IllegalArgumentException("사용할 수 없는 계정입니다.");
        }

        // JWT 토큰 발급
        TokenResponse tokens = jwtService.issueTokens(member);

        log.info("로그인 완료: email={}", request.getEmail());

        return LoginResponse.of(tokens, memberMapper.toDto(member));
    }

    /**
     * 로그아웃
     */
    @Transactional
    public void logout(Long memberId) {
        jwtService.logout(memberId);
    }

    /**
     * Access Token 재발급
     */
    @Transactional
    public TokenResponse refresh(String refreshToken) {
        return jwtService.refreshAccessToken(refreshToken);
    }
}
