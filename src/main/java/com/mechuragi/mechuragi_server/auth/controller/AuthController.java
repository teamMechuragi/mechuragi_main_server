package com.mechuragi.mechuragi_server.auth.controller;

import com.mechuragi.mechuragi_server.auth.dto.*;
import com.mechuragi.mechuragi_server.auth.service.AuthService;
import com.mechuragi.mechuragi_server.auth.service.EmailService;
import com.mechuragi.mechuragi_server.domain.member.dto.MemberResponse;
import com.mechuragi.mechuragi_server.global.util.NicknameGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "인증/인가", description = "인증/인가 API")
public class AuthController {

    private final AuthService authService;
    private final EmailService emailService;
    private final NicknameGenerator nicknameGenerator;

    @PostMapping("/signup")
    public ResponseEntity<MemberResponse> signup(@Valid @RequestBody SignupRequest request) {
        log.info("회원가입 요청: email={}", request.getEmail());
        MemberResponse response = authService.signup(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "로그인")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("로그인 요청: email={}", request.getEmail());
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("로그아웃 요청: memberId={}", userDetails.getMemberId());
        authService.logout(userDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "토큰 재발급 요청")
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestHeader("Refresh-Token") String refreshToken) {
        log.info("토큰 재발급 요청");
        TokenResponse response = authService.refresh(refreshToken);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "인증 이메일 발송 요청")
    @PostMapping("/email/send")
    public ResponseEntity<Void> sendVerificationEmail(@Valid @RequestBody SendVerificationEmailRequest request) {
        log.info("이메일 인증 메일 발송 요청: email={}", request.getEmail());
        emailService.sendVerificationEmail(request.getEmail());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "이메일 검증 요청")
    @PostMapping("/email/verify")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        log.info("이메일 인증 요청: email={}", request.getEmail());
        emailService.verifyEmail(request.getEmail(), request.getVerificationCode());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "랜덤 닉네임 생성")
    @GetMapping("/nickname/generate")
    public ResponseEntity<NicknameResponse> generateNickname() {
        log.info("랜덤 닉네임 생성 요청");
        String nickname = nicknameGenerator.generateRandomNickname();
        return ResponseEntity.ok(new NicknameResponse(nickname));
    }
}
