package com.mechuragi.mechuragi_server.domain.preference.controller;

import com.mechuragi.mechuragi_server.domain.preference.dto.*;
import com.mechuragi.mechuragi_server.domain.preference.service.FoodPreferenceService;
import com.mechuragi.mechuragi_server.domain.member.entity.Member;
import com.mechuragi.mechuragi_server.domain.member.repository.MemberRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/preferences")
@RequiredArgsConstructor
public class FoodPreferenceController {

    private final FoodPreferenceService foodPreferenceService;
    private final MemberRepository memberRepository;

    // 음식 취향 등록
    @PostMapping
    public ResponseEntity<Void> createPreference(
            // 실제 인증 구현 후 @AuthenticationPrincipal Member member로 변경
            @RequestParam Long memberId,
            @Valid @RequestBody CreatePreferenceRequest request) {

        // 실제 Member 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Long preferenceId = foodPreferenceService.createPreference(member, request);
        return ResponseEntity.created(URI.create("/api/preferences/" + preferenceId)).build();
    }

    // 음식 취향 목록 조회
    @GetMapping
    public ResponseEntity<List<PreferenceListResponse>> getPreferenceList(
            // 실제 인증 구현 후 @AuthenticationPrincipal Member member로 변경
            @RequestParam Long memberId) {

        List<PreferenceListResponse> preferences = foodPreferenceService.getPreferenceList(memberId);
        return ResponseEntity.ok(preferences);
    }

    // 음식 취향 상세 조회
    @GetMapping("/{preferenceId}")
    public ResponseEntity<PreferenceDetailResponse> getPreferenceDetail(
            // 실제 인증 구현 후 @AuthenticationPrincipal Member member로 변경
            @RequestParam Long memberId,
            @PathVariable Long preferenceId) {

        PreferenceDetailResponse preference = foodPreferenceService.getPreferenceDetail(memberId, preferenceId);
        return ResponseEntity.ok(preference);
    }

    // 음식 취향 수정
    @PutMapping("/{preferenceId}")
    public ResponseEntity<Void> updatePreference(
            // 실제 인증 구현 후 @AuthenticationPrincipal Member member로 변경
            @RequestParam Long memberId,
            @PathVariable Long preferenceId,
            @Valid @RequestBody UpdatePreferenceRequest request) {

        foodPreferenceService.updatePreference(memberId, preferenceId, request);
        return ResponseEntity.ok().build();
    }

    // 음식 취향 삭제
    @DeleteMapping("/{preferenceId}")
    public ResponseEntity<Void> deletePreference(
            // 실제 인증 구현 후 @AuthenticationPrincipal Member member로 변경
            @RequestParam Long memberId,
            @PathVariable Long preferenceId) {

        foodPreferenceService.deletePreference(memberId, preferenceId);
        return ResponseEntity.noContent().build();
    }
}