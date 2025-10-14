package com.mechuragi.mechuragi_server.domain.member.controller;

import com.mechuragi.mechuragi_server.domain.member.dto.MemberResponse;
import com.mechuragi.mechuragi_server.domain.member.dto.UpdateMemberRequest;
import com.mechuragi.mechuragi_server.domain.member.dto.UpdatePasswordRequest;
import com.mechuragi.mechuragi_server.domain.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    // 회원 정보 조회
    @GetMapping("/{memberId}")
    public ResponseEntity<MemberResponse> getMember(@PathVariable Long memberId) {
        MemberResponse member = memberService.getMember(memberId);
        return ResponseEntity.ok(member);
    }

    // 회원 정보 수정
    @PutMapping("/{memberId}")
    public ResponseEntity<MemberResponse> updateMember(
            @PathVariable Long memberId,
            @Valid @RequestBody UpdateMemberRequest request) {
        MemberResponse updatedMember = memberService.updateMember(memberId, request);
        return ResponseEntity.ok(updatedMember);
    }

    // 비밀번호 변경
    @PutMapping("/{memberId}/password")
    public ResponseEntity<Void> updatePassword(
            @PathVariable Long memberId,
            @Valid @RequestBody UpdatePasswordRequest request) {
        memberService.updatePassword(memberId, request);
        return ResponseEntity.ok().build();
    }

    // 회원 탈퇴
    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> deleteMember(@PathVariable Long memberId) {
        memberService.deleteMember(memberId);
        return ResponseEntity.noContent().build();
    }

    // 이메일 중복 체크
    @GetMapping("/check/email")
    public ResponseEntity<Boolean> checkEmailDuplicate(@RequestParam String email) {
        boolean isDuplicate = memberService.isEmailDuplicate(email);
        return ResponseEntity.ok(isDuplicate);
    }

    // 닉네임 중복 체크
    @GetMapping("/check/nickname")
    public ResponseEntity<Boolean> checkNicknameDuplicate(@RequestParam String nickname) {
        boolean isDuplicate = memberService.isNicknameDuplicate(nickname);
        return ResponseEntity.ok(isDuplicate);
    }
}
