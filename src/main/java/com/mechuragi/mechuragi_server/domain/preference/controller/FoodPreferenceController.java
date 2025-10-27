package com.mechuragi.mechuragi_server.domain.preference.controller;

import com.mechuragi.mechuragi_server.domain.preference.dto.*;
import com.mechuragi.mechuragi_server.domain.preference.service.FoodPreferenceService;
import com.mechuragi.mechuragi_server.domain.member.entity.Member;
import com.mechuragi.mechuragi_server.domain.member.repository.MemberRepository;
import com.mechuragi.mechuragi_server.auth.util.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
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
    private final JwtTokenProvider jwtTokenProvider;

    private Long getMemberIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return jwtTokenProvider.getMemberIdFromToken(token);
        }
        throw new IllegalArgumentException("유효한 JWT 토큰이 없습니다");
    }

    // 음식 취향 등록
    @PostMapping
    public ResponseEntity<Void> createPreference(
            HttpServletRequest request,
            @Valid @RequestBody CreatePreferenceRequest preferenceRequest) {

        Long memberId = getMemberIdFromRequest(request);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Long preferenceId = foodPreferenceService.createPreference(member, preferenceRequest);
        return ResponseEntity.created(URI.create("/api/preferences/" + preferenceId)).build();
    }

    // 음식 취향 목록 조회
    @GetMapping
    public ResponseEntity<List<PreferenceListResponse>> getPreferenceList(
            HttpServletRequest request) {

        Long memberId = getMemberIdFromRequest(request);
        List<PreferenceListResponse> preferences = foodPreferenceService.getPreferenceList(memberId);
        return ResponseEntity.ok(preferences);
    }

    // 음식 취향 상세 조회
    @GetMapping("/{preferenceId}")
    public ResponseEntity<PreferenceDetailResponse> getPreferenceDetail(
            HttpServletRequest request,
            @PathVariable Long preferenceId) {

        Long memberId = getMemberIdFromRequest(request);
        PreferenceDetailResponse preference = foodPreferenceService.getPreferenceDetail(memberId, preferenceId);
        return ResponseEntity.ok(preference);
    }

    // 음식 취향 수정
    @PutMapping("/{preferenceId}")
    public ResponseEntity<Void> updatePreference(
            HttpServletRequest request,
            @PathVariable Long preferenceId,
            @Valid @RequestBody UpdatePreferenceRequest updateRequest) {

        Long memberId = getMemberIdFromRequest(request);
        foodPreferenceService.updatePreference(memberId, preferenceId, updateRequest);
        return ResponseEntity.ok().build();
    }

    // 음식 취향 삭제
    @DeleteMapping("/{preferenceId}")
    public ResponseEntity<Void> deletePreference(
            HttpServletRequest request,
            @PathVariable Long preferenceId) {

        Long memberId = getMemberIdFromRequest(request);
        foodPreferenceService.deletePreference(memberId, preferenceId);
        return ResponseEntity.noContent().build();
    }
}