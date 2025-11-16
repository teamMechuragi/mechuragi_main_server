package com.mechuragi.mechuragi_server.domain.preference.controller;

import com.mechuragi.mechuragi_server.auth.dto.CustomUserDetails;
import com.mechuragi.mechuragi_server.domain.preference.dto.*;
import com.mechuragi.mechuragi_server.domain.preference.service.FoodPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/preferences")
@RequiredArgsConstructor
@Tag(name = "음식 취향",description = "사용자 음식 취향 관리 API")
public class FoodPreferenceController {

    private final FoodPreferenceService foodPreferenceService;

    @PostMapping
    @Operation(summary = "음식 취향 등록")
    public ResponseEntity<Void> createPreference(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreatePreferenceRequestDTO preferenceRequest) {

        Long memberId = userDetails.getMemberId();
        Long preferenceId = foodPreferenceService.createPreference(memberId, preferenceRequest);
        return ResponseEntity.created(URI.create("/api/preferences/" + preferenceId)).build();
    }

    @GetMapping
    @Operation(summary = "음식 취향 목록 조회")
    public ResponseEntity<List<PreferenceListResponseDTO>> getPreferenceList(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long memberId = userDetails.getMemberId();
        List<PreferenceListResponseDTO> preferences = foodPreferenceService.getPreferenceList(memberId);
        return ResponseEntity.ok(preferences);
    }

    @Operation(summary = "음식 취향 상세 조회")
    @GetMapping("/{preferenceId}")
    public ResponseEntity<PreferenceDetailResponseDTO> getPreferenceDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long preferenceId) {

        Long memberId = userDetails.getMemberId();
        PreferenceDetailResponseDTO preference = foodPreferenceService.getPreferenceDetail(memberId, preferenceId);
        return ResponseEntity.ok(preference);
    }

    @Operation(summary = "음식 취향 수정")
    @PutMapping("/{preferenceId}")
    public ResponseEntity<Void> updatePreference(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long preferenceId,
            @Valid @RequestBody UpdatePreferenceRequestDTO updateRequest) {

        Long memberId = userDetails.getMemberId();
        foodPreferenceService.updatePreference(memberId, preferenceId, updateRequest);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "음식 취향 삭제")
    @DeleteMapping("/{preferenceId}")
    public ResponseEntity<Void> deletePreference(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long preferenceId) {

        Long memberId = userDetails.getMemberId();
        foodPreferenceService.deletePreference(memberId, preferenceId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "음식 취향 토글 상태 변경")
    @PatchMapping("/{preferenceId}/toggle-active")
    public ResponseEntity<Void> toggleActivePreference(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long preferenceId) {

        Long memberId = userDetails.getMemberId();
        foodPreferenceService.toggleActivePreference(memberId, preferenceId);
        return ResponseEntity.ok().build();
    }
}