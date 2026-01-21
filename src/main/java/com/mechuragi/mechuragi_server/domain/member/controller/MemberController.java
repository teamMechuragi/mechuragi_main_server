package com.mechuragi.mechuragi_server.domain.member.controller;

import com.mechuragi.mechuragi_server.auth.dto.CustomUserDetails;
import com.mechuragi.mechuragi_server.domain.member.dto.MemberResponse;
import com.mechuragi.mechuragi_server.domain.member.dto.SignupRequest;
import com.mechuragi.mechuragi_server.domain.member.dto.UpdateMemberRequest;
import com.mechuragi.mechuragi_server.domain.member.dto.UpdatePasswordRequest;
import com.mechuragi.mechuragi_server.domain.member.dto.UpdateNotificationSettingRequest;
import com.mechuragi.mechuragi_server.domain.member.service.MemberService;
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
@RequestMapping("/api/members")
@RequiredArgsConstructor
@Tag(name = "회원", description = "회원 정보 및 상태를 관리합니다.")
public class MemberController {

    private final MemberService memberService;

    // 로그인 전
    @Operation(summary = "회원가입")
    @PostMapping("/signup")
    public ResponseEntity<MemberResponse> signup(@Valid @RequestBody SignupRequest request) {
        log.info("회원가입 요청: email={}", request.getEmail());
        MemberResponse response = memberService.signup(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "이메일 중복 체크")
    @GetMapping("/check/email")
    public ResponseEntity<Boolean> checkEmailDuplicate(@RequestParam String email) {
        log.info("이메일 중복 체크 요청: email={}", email);
        boolean isDuplicate = memberService.isEmailDuplicate(email);
        return ResponseEntity.ok(isDuplicate);
    }

    @Operation(summary = "닉네임 중복 체크")
    @GetMapping("/check/nickname")
    public ResponseEntity<Boolean> checkNicknameDuplicate(@RequestParam String nickname) {
        boolean isDuplicate = memberService.isNicknameDuplicate(nickname);
        return ResponseEntity.ok(isDuplicate);
    }

    // 로그인 후 - 내 정보 관리
    @Operation(summary = "내 회원 정보 조회")
    @GetMapping("/me")
    public ResponseEntity<MemberResponse> getCurrentMember(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails.getMemberId();
        MemberResponse member = memberService.getMember(memberId);
        return ResponseEntity.ok(member);
    }


    @Operation(summary = "내 회원 정보 수정")
    @PutMapping("/me/profile")
    public ResponseEntity<MemberResponse> updateMember(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateMemberRequest request) {
        Long memberId = userDetails.getMemberId();
        MemberResponse updatedMember = memberService.updateMember(memberId, request);
        return ResponseEntity.ok(updatedMember);
    }

    @Operation(summary = "내 비밀번호 변경")
    @PutMapping("/me/password")
    public ResponseEntity<Void> updatePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdatePasswordRequest request) {
        Long memberId = userDetails.getMemberId();
        memberService.updatePassword(memberId, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "회원 탈퇴")
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMember(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails.getMemberId();
        memberService.deleteMember(memberId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/notification-setting")
    @Operation(summary = "내 알림 설정 상태 변경", description = "알림 설정 상태를 변경합니다.")
    public ResponseEntity<Void> updateVoteNotificationSetting(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateNotificationSettingRequest request) {
        Long memberId = userDetails.getMemberId();
        memberService.updateVoteNotificationSetting(memberId, request.getEnabled());
        return ResponseEntity.ok().build();
    }

    // 다른 회원 정보 관리

    @Operation(summary = "타인 및 공개된 회원 정보 조회")
    @GetMapping("/{memberId}")
    public ResponseEntity<MemberResponse> getMember(@PathVariable Long memberId) {
        MemberResponse member = memberService.getMember(memberId);
        return ResponseEntity.ok(member);
    }

}
