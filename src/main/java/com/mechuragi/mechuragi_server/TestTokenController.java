package com.mechuragi.mechuragi_server;

import com.mechuragi.mechuragi_server.auth.util.JwtTokenProvider;
import com.mechuragi.mechuragi_server.domain.member.entity.Member;
import com.mechuragi.mechuragi_server.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로컬 부하 테스트용 임시 토큰 발급 엔드포인트
 * local 프로필에서만 활성화됨
 */
@RestController
@RequiredArgsConstructor
@Profile("local")
public class TestTokenController {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;

    @GetMapping(value = "/test/token", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> token() {
        Member member = memberRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("DB에 유저가 없습니다. 먼저 회원가입을 해주세요."));

        String accessToken = jwtTokenProvider.generateAccessToken(
                member.getEmail(), member.getId(), member.getRole().name());
        return ResponseEntity.ok("Bearer " + accessToken);
    }
}
