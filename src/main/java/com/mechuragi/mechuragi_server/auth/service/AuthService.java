package com.mechuragi.mechuragi_server.auth.service;

import com.mechuragi.mechuragi_server.auth.dto.LoginRequest;
import com.mechuragi.mechuragi_server.auth.dto.LoginResponse;
import com.mechuragi.mechuragi_server.auth.dto.SignupRequest;
import com.mechuragi.mechuragi_server.auth.dto.TokenResponse;
import com.mechuragi.mechuragi_server.domain.member.dto.MemberResponse;
import com.mechuragi.mechuragi_server.domain.member.entity.Member;
import com.mechuragi.mechuragi_server.domain.member.entity.type.AuthProvider;
import com.mechuragi.mechuragi_server.domain.member.entity.type.MemberStatus;
import com.mechuragi.mechuragi_server.domain.member.entity.type.Role;
import com.mechuragi.mechuragi_server.domain.member.repository.MemberRepository;
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
    private final EmailService emailService;

    /**
     * 일반 회원가입
     */
    @Transactional
    public MemberResponse signup(SignupRequest request) {
        // 이메일 중복 체크
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 닉네임 중복 체크
        if (memberRepository.existsByNickname(request.getNickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 회원 엔티티 생성
        Member member = Member.builder()
                .email(request.getEmail())
                .password(encodedPassword)
                .nickname(request.getNickname())
                .emailVerified(false)
                .provider(AuthProvider.NORMAL)
                .role(Role.USER)
                .status(MemberStatus.ACTIVE)
                .build();

        // 회원 저장
        Member savedMember = memberRepository.save(member);

        log.info("회원가입 완료: email={}, nickname={}", request.getEmail(), request.getNickname());

        // 이메일 인증 메일 발송
        try {
            emailService.sendVerificationEmail(request.getEmail());
        } catch (Exception e) {
            log.error("이메일 인증 메일 발송 실패: email={}, error={}", request.getEmail(), e.getMessage());
            // 이메일 발송 실패해도 회원가입은 완료됨 (나중에 재발송 가능)
        }

        return MemberResponse.from(savedMember);
    }

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

        return LoginResponse.of(tokens, MemberResponse.from(member));
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
