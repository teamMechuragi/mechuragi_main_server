package com.mechuragi.mechuragi_server.domain.member.controller;

import com.mechuragi.mechuragi_server.auth.dto.CustomUserDetails;
import com.mechuragi.mechuragi_server.domain.member.dto.MemberResponse;
import com.mechuragi.mechuragi_server.domain.member.dto.NicknameResponse;
import com.mechuragi.mechuragi_server.domain.member.dto.SignupRequest;
import com.mechuragi.mechuragi_server.domain.member.dto.UpdateMemberRequest;
import com.mechuragi.mechuragi_server.domain.member.dto.UpdatePasswordRequest;
import com.mechuragi.mechuragi_server.domain.member.dto.UpdateNotificationSettingRequest;
import com.mechuragi.mechuragi_server.domain.member.service.MemberService;
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
@RequestMapping("/api/members")
@RequiredArgsConstructor
@Tag(name = "회원", description = "회원 정보 및 상태를 관리합니다.")
public class MemberController {

    private final MemberService memberService;
    private final NicknameGenerator nicknameGenerator;

    @Operation(summary = "회원가입")
    @PostMapping("/signup")
    public ResponseEntity<MemberResponse> signup(@Valid @RequestBody SignupRequest request) {
        log.info("회원가입 요청: email={}", request.getEmail());
        MemberResponse response = memberService.signup(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "랜덤 닉네임 생성")
    @GetMapping("/nickname/generate")
    public ResponseEntity<NicknameResponse> generateNickname() {
        log.info("랜덤 닉네임 생성 요청");
        String nickname = nicknameGenerator.generateRandomNickname();
        return ResponseEntity.ok(new NicknameResponse(nickname));
    }

    @Operation(summary = "이메일 중복 체크")
    @GetMapping("/check")
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

    @Operation(summary = "회원 정보 조회")
    @GetMapping("/{memberId}")
    public ResponseEntity<MemberResponse> getMember(@PathVariable Long memberId) {
        MemberResponse member = memberService.getMember(memberId);
        return ResponseEntity.ok(member);
    }

    @Operation(summary = "회원 정보 수정")
    @PutMapping("/{memberId}")
    public ResponseEntity<MemberResponse> updateMember(
            @PathVariable Long memberId,
            @Valid @RequestBody UpdateMemberRequest request) {
        MemberResponse updatedMember = memberService.updateMember(memberId, request);
        return ResponseEntity.ok(updatedMember);
    }

    @Operation(summary = "비밀번호 변경")
    @PutMapping("/{memberId}/password")
    public ResponseEntity<Void> updatePassword(
            @PathVariable Long memberId,
            @Valid @RequestBody UpdatePasswordRequest request) {
        memberService.updatePassword(memberId, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "회원 탈퇴")
    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> deleteMember(@PathVariable Long memberId) {
        memberService.deleteMember(memberId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/notification-setting")
    @Operation(summary = "알림 설정 상태 변경", description = "알림 설정 상태를 변경합니다.")
    public ResponseEntity<Void> updateVoteNotificationSetting(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateNotificationSettingRequest request) {
        Long memberId = userDetails.getMemberId();
        memberService.updateVoteNotificationSetting(memberId, request.getEnabled());
        return ResponseEntity.ok().build();
    }
}
