package com.mechuragi.mechuragi_server.auth.controller;

import com.mechuragi.mechuragi_server.auth.dto.CustomUserDetails;
import com.mechuragi.mechuragi_server.auth.dto.LoginRequest;
import com.mechuragi.mechuragi_server.auth.dto.LoginResponse;
import com.mechuragi.mechuragi_server.auth.dto.SignupRequest;
import com.mechuragi.mechuragi_server.auth.dto.TokenResponse;
import com.mechuragi.mechuragi_server.auth.service.AuthService;
import com.mechuragi.mechuragi_server.domain.member.dto.MemberResponse;
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
public class AuthController {

    private final AuthService authService;

    /**
     * 회원가입
     */
    @PostMapping("/signup")
    public ResponseEntity<MemberResponse> signup(@Valid @RequestBody SignupRequest request) {
        log.info("회원가입 요청: email={}", request.getEmail());
        MemberResponse response = authService.signup(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("로그인 요청: email={}", request.getEmail());
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 로그아웃
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("로그아웃 요청: memberId={}", userDetails.getMemberId());
        authService.logout(userDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Access Token 재발급
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestHeader("Refresh-Token") String refreshToken) {
        log.info("토큰 재발급 요청");
        TokenResponse response = authService.refresh(refreshToken);
        return ResponseEntity.ok(response);
    }
}
