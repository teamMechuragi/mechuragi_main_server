package com.mechuragi.mechuragi_server.domain.preference.controller;

import com.mechuragi.mechuragi_server.domain.preference.dto.*;
import com.mechuragi.mechuragi_server.domain.preference.service.FoodPreferenceService;
import com.mechuragi.mechuragi_server.domain.user.entity.User;
import com.mechuragi.mechuragi_server.domain.user.repository.UserRepository;
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
    private final UserRepository userRepository;

    // 음식 취향 등록
    @PostMapping
    public ResponseEntity<Void> createPreference(
            // 실제 인증 구현 후 @AuthenticationPrincipal User user로 변경
            @RequestParam Long userId,
            @Valid @RequestBody CreatePreferenceRequest request) {

        // 실제 User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Long preferenceId = foodPreferenceService.createPreference(user, request);
        return ResponseEntity.created(URI.create("/api/preferences/" + preferenceId)).build();
    }

    // 음식 취향 목록 조회
    @GetMapping
    public ResponseEntity<List<PreferenceListResponse>> getPreferenceList(
            // 실제 인증 구현 후 @AuthenticationPrincipal User user로 변경
            @RequestParam Long userId) {

        List<PreferenceListResponse> preferences = foodPreferenceService.getPreferenceList(userId);
        return ResponseEntity.ok(preferences);
    }

    // 음식 취향 상세 조회
    @GetMapping("/{preferenceId}")
    public ResponseEntity<PreferenceDetailResponse> getPreferenceDetail(
            // 실제 인증 구현 후 @AuthenticationPrincipal User user로 변경
            @RequestParam Long userId,
            @PathVariable Long preferenceId) {

        PreferenceDetailResponse preference = foodPreferenceService.getPreferenceDetail(userId, preferenceId);
        return ResponseEntity.ok(preference);
    }

    // 음식 취향 수정
    @PutMapping("/{preferenceId}")
    public ResponseEntity<Void> updatePreference(
            // 실제 인증 구현 후 @AuthenticationPrincipal User user로 변경
            @RequestParam Long userId,
            @PathVariable Long preferenceId,
            @Valid @RequestBody UpdatePreferenceRequest request) {

        foodPreferenceService.updatePreference(userId, preferenceId, request);
        return ResponseEntity.ok().build();
    }

    // 음식 취향 삭제
    @DeleteMapping("/{preferenceId}")
    public ResponseEntity<Void> deletePreference(
            // 실제 인증 구현 후 @AuthenticationPrincipal User user로 변경
            @RequestParam Long userId,
            @PathVariable Long preferenceId) {

        foodPreferenceService.deletePreference(userId, preferenceId);
        return ResponseEntity.noContent().build();
    }
}