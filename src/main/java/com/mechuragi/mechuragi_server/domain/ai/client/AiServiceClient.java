package com.mechuragi.mechuragi_server.domain.ai.client;

import com.mechuragi.mechuragi_server.domain.ai.dto.FoodRecommendationRequest;
import com.mechuragi.mechuragi_server.domain.ai.dto.FoodRecommendationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiServiceClient {

    @Value("${ai-service.url:http://localhost:8082}")
    private String aiServiceUrl;

    private final RestTemplate restTemplate;

    public FoodRecommendationResponse getFoodRecommendation(FoodRecommendationRequest request) {
        try {
            String url = aiServiceUrl + "/api/ai/recommend";
            log.info("AI 서비스 호출: {} - {}", url, request.getType());

            FoodRecommendationResponse response = restTemplate.postForObject(
                url,
                request,
                FoodRecommendationResponse.class
            );

            log.info("AI 서비스 응답 수신: {} 개 추천",
                response != null && response.getRecommendations() != null ?
                response.getRecommendations().size() : 0);

            return response;
        } catch (Exception e) {
            log.error("AI 서비스 호출 실패", e);
            throw new RuntimeException("AI 추천 서비스와 통신 중 오류가 발생했습니다", e);
        }
    }
}