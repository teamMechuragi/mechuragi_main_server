package com.mechuragi.mechuragi_server.domain.ai.controller;

import com.mechuragi.mechuragi_server.auth.dto.CustomUserDetails;
import com.mechuragi.mechuragi_server.domain.ai.dto.common.response.RecommendedFoodResponse;
import com.mechuragi.mechuragi_server.domain.ai.dto.internal.request.SaveRecommendedFoodsRequest;
import com.mechuragi.mechuragi_server.domain.ai.service.RecommendedFoodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai/recommended-foods")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI 추천 음식 관리", description = "AI가 추천한 음식 조회 및 스크랩 관리 API")
public class RecommendedFoodController {

    private final RecommendedFoodService recommendedFoodService;

    @PostMapping("/save")
    @Operation(summary = "추천 결과 저장 (AI 서버 전용)", description = "AI 서버에서 생성한 추천 결과를 저장합니다")
    public ResponseEntity<Void> saveRecommendations(@Valid @RequestBody SaveRecommendedFoodsRequest request) {
        log.info("추천 결과 저장 요청 - 회원: {}, 개수: {}",
                request.getMemberId(), request.getRecommendations().size());

        recommendedFoodService.saveRecommendations(request.getMemberId(), request.getRecommendations());
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @Operation(summary = "모든 추천 음식 조회", description = "사용자의 모든 AI 추천 음식 목록을 조회합니다")
    public ResponseEntity<List<RecommendedFoodResponse>> getAllRecommendations(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long memberId = userDetails.getMemberId();
        log.info("전체 추천 음식 조회 - 회원: {}", memberId);

        List<RecommendedFoodResponse> recommendations = recommendedFoodService.getAllRecommendations(memberId);
        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/scrapped")
    @Operation(summary = "스크랩한 추천 음식 조회", description = "사용자가 스크랩한 AI 추천 음식 목록만 조회합니다")
    public ResponseEntity<List<RecommendedFoodResponse>> getScrappedRecommendations(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long memberId = userDetails.getMemberId();
        log.info("스크랩 추천 음식 조회 - 회원: {}", memberId);

        List<RecommendedFoodResponse> scrappedRecommendations =
                recommendedFoodService.getScrappedRecommendations(memberId);
        return ResponseEntity.ok(scrappedRecommendations);
    }

    @PostMapping("/{recommendedFoodId}/scrap")
    @Operation(summary = "추천 음식 스크랩 토글", description = "추천 음식의 스크랩 상태를 토글합니다")
    public ResponseEntity<Void> toggleScrap(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long recommendedFoodId) {

        Long memberId = userDetails.getMemberId();
        log.info("추천 음식 스크랩 토글 - 회원: {}, 추천 ID: {}", memberId, recommendedFoodId);

        recommendedFoodService.toggleScrap(memberId, recommendedFoodId);
        return ResponseEntity.ok().build();
    }
}
