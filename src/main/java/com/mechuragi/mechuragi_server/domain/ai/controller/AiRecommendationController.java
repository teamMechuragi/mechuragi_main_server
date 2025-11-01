package com.mechuragi.mechuragi_server.domain.ai.controller;

import com.mechuragi.mechuragi_server.auth.dto.CustomUserDetails;
import com.mechuragi.mechuragi_server.domain.ai.dto.common.response.FoodRecommendationResponse;
import com.mechuragi.mechuragi_server.domain.ai.dto.internal.request.*;
import com.mechuragi.mechuragi_server.domain.ai.service.AiRecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/recommend")
@RequiredArgsConstructor
@Slf4j
public class AiRecommendationController {

    private final AiRecommendationService aiRecommendationService;

    @PostMapping("/weather")
    public ResponseEntity<FoodRecommendationResponse> getWeatherBasedRecommendation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody WeatherRecommendationRequest request) {

        Long memberId = userDetails.getMemberId();
        log.info("날씨 기반 추천 요청 - 사용자: {}, 날씨: {}", memberId, request.getWeatherConditions());

        FoodRecommendationResponse response = aiRecommendationService.getWeatherBasedRecommendation(
                memberId, request.getWeatherConditions());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/time")
    public ResponseEntity<FoodRecommendationResponse> getTimeBasedRecommendation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody TimeRecommendationRequest request) {

        Long memberId = userDetails.getMemberId();
        log.info("시간 기반 추천 요청 - 사용자: {}, 시간: {}", memberId, request.getTimeOfDay());

        FoodRecommendationResponse response = aiRecommendationService.getTimeBasedRecommendation(
                memberId, request.getTimeOfDay());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/ingredients")
    public ResponseEntity<FoodRecommendationResponse> getIngredientsBasedRecommendation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody IngredientsRecommendationRequest request) {

        Long memberId = userDetails.getMemberId();
        log.info("재료 기반 추천 요청 - 사용자: {}, 재료: {}", memberId, request.getIngredients());

        FoodRecommendationResponse response = aiRecommendationService.getIngredientsBasedRecommendation(
                memberId, request.getIngredients());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/feeling")
    public ResponseEntity<FoodRecommendationResponse> getFeelingBasedRecommendation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody FeelingRecommendationRequest request) {

        Long memberId = userDetails.getMemberId();
        log.info("기분 기반 추천 요청 - 사용자: {}, 기분: {}", memberId, request.getFeeling());

        FoodRecommendationResponse response = aiRecommendationService.getFeelingBasedRecommendation(
                memberId, request.getFeeling());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/conversation")
    public ResponseEntity<FoodRecommendationResponse> getConversationBasedRecommendation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ConversationRecommendationRequest request) {

        Long memberId = userDetails.getMemberId();
        log.info("대화 기반 추천 요청 - 사용자: {}, 메시지: {}", memberId, request.getMessage());

        FoodRecommendationResponse response = aiRecommendationService.getConversationBasedRecommendation(
                memberId, request.getMessage());

        return ResponseEntity.ok(response);
    }
}
