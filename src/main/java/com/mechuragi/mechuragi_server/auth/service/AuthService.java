package com.mechuragi.mechuragi_server.auth.service;

import com.mechuragi.mechuragi_server.auth.dto.LoginRequest;
import com.mechuragi.mechuragi_server.auth.dto.LoginResponse;
import com.mechuragi.mechuragi_server.auth.dto.SignupRequest;
import com.mechuragi.mechuragi_server.auth.dto.TokenResponse;
import com.mechuragi.mechuragi_server.auth.entity.EmailVerification;
import com.mechuragi.mechuragi_server.auth.repository.EmailVerificationRepository;
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
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public MemberResponse signup(SignupRequest request) {
        // 이메일 중복 체크
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // TODO: AWS SES production 승인 대기 중 - 이메일 인증 로직 임시 비활성화
        // 이메일 인증 여부 확인
        /*
        EmailVerification emailVerification = emailVerificationRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 인증이 필요합니다."));

        if (!emailVerification.getVerified()) {
            throw new IllegalArgumentException("이메일 인증이 완료되지 않았습니다.");
        }

        if (emailVerification.isExpired()) {
            throw new IllegalArgumentException("이메일 인증이 만료되었습니다. 다시 인증해주세요.");
        }
        */

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 회원 엔티티 생성 (임시 닉네임으로 먼저 저장)
        Member member = Member.builder()
                .email(request.getEmail())
                .password(encodedPassword)
                .nickname(request.getNickname()) // 임시 닉네임
                .emailVerified(true) // 이메일 인증 완료
                .provider(AuthProvider.NORMAL)
                .role(Role.USER)
                .status(MemberStatus.ACTIVE)
                .build();

        // 회원 저장 (ID 발급을 위해)
        Member savedMember = memberRepository.save(member);

        // 닉네임 + ID 조합으로 최종 닉네임 업데이트 (예: "행복한곰1")
        savedMember.appendIdToNickname(request.getNickname());

        // TODO: AWS SES production 승인 대기 중 - 이메일 인증 정보 삭제 로직 임시 비활성화
        // 이메일 인증 정보 삭제 (회원가입 완료 후)
        // emailVerificationRepository.delete(emailVerification);

        log.info("회원가입 완료: email={}, nickname={}", request.getEmail(), savedMember.getNickname());

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
