package com.mechuragi.mechuragi_server.domain.ai.controller;

import com.mechuragi.mechuragi_server.domain.ai.dto.FoodRecommendationRequest;
import com.mechuragi.mechuragi_server.domain.ai.dto.FoodRecommendationResponse;
import com.mechuragi.mechuragi_server.domain.ai.service.AiRecommendationService;
import com.mechuragi.mechuragi_server.auth.util.JwtTokenProvider;
import com.mechuragi.mechuragi_server.global.exception.BusinessException;
import com.mechuragi.mechuragi_server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/ai-recommendations")
@RequiredArgsConstructor
@Slf4j
public class AiRecommendationController {

    private final AiRecommendationService aiRecommendationService;
    private final JwtTokenProvider jwtTokenProvider;

    private Long getMemberIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return jwtTokenProvider.getMemberIdFromToken(token);
        }
        throw new BusinessException(ErrorCode.INVALID_JWT_TOKEN);
    }

    @PostMapping("/weather")
    public ResponseEntity<FoodRecommendationResponse> getWeatherBasedRecommendation(
            HttpServletRequest request,
            @RequestBody WeatherRequest weatherRequest) {

        Long memberId = getMemberIdFromRequest(request);
        log.info("날씨 기반 추천 요청 - 사용자: {}, 날씨: {}", memberId, weatherRequest.getWeatherConditions());

        FoodRecommendationResponse response = aiRecommendationService.getWeatherBasedRecommendation(
            memberId, weatherRequest.getWeatherConditions());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/time")
    public ResponseEntity<FoodRecommendationResponse> getTimeBasedRecommendation(
            HttpServletRequest request,
            @RequestBody TimeRequest timeRequest) {

        Long memberId = getMemberIdFromRequest(request);
        log.info("시간 기반 추천 요청 - 사용자: {}, 시간: {}", memberId, timeRequest.getTimeOfDay());

        FoodRecommendationResponse response = aiRecommendationService.getTimeBasedRecommendation(
            memberId, timeRequest.getTimeOfDay());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/conversation")
    public ResponseEntity<FoodRecommendationResponse> getConversationBasedRecommendation(
            HttpServletRequest request,
            @RequestBody ConversationRequest conversationRequest) {

        Long memberId = getMemberIdFromRequest(request);
        log.info("대화 기반 추천 요청 - 사용자: {}, 메시지: {}", memberId, conversationRequest.getMessage());

        FoodRecommendationResponse response = aiRecommendationService.getConversationBasedRecommendation(
            memberId, conversationRequest.getMessage());

        return ResponseEntity.ok(response);
    }

    public static class WeatherRequest {
        private List<String> weatherConditions;

        public List<String> getWeatherConditions() { return weatherConditions; }
        public void setWeatherConditions(List<String> weatherConditions) { this.weatherConditions = weatherConditions; }
    }

    public static class TimeRequest {
        private String timeOfDay;

        public String getTimeOfDay() { return timeOfDay; }
        public void setTimeOfDay(String timeOfDay) { this.timeOfDay = timeOfDay; }
    }

    public static class ConversationRequest {
        private String message;

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}