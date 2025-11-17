package com.mechuragi.mechuragi_server.auth.controller;

import com.mechuragi.mechuragi_server.auth.dto.SendVerificationEmailRequest;
import com.mechuragi.mechuragi_server.auth.dto.VerifyEmailRequest;
import com.mechuragi.mechuragi_server.auth.service.EmailService;
import com.mechuragi.mechuragi_server.domain.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
@Tag(name = "이메일", description = "이메일 인증 및 검증 API")
public class EmailController {

    private final EmailService emailService;

    @Operation(summary = "인증 이메일 발송 요청")
    @PostMapping("/send")
    public ResponseEntity<Void> sendVerificationEmail(@Valid @RequestBody SendVerificationEmailRequest request) {
        log.info("이메일 인증 메일 발송 요청: email={}", request.getEmail());
        emailService.sendVerificationEmail(request.getEmail());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "이메일 검증 요청")
    @PostMapping("/verify")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        log.info("이메일 인증 요청: email={}", request.getEmail());
        emailService.verifyEmail(request.getEmail(), request.getVerificationCode());
        return ResponseEntity.ok().build();
    }
}
