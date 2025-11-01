package com.mechuragi.mechuragi_server.domain.ai.client;

import com.mechuragi.mechuragi_server.domain.ai.dto.external.request.FoodRecommendationRequest;
import com.mechuragi.mechuragi_server.domain.ai.dto.common.response.FoodRecommendationResponse;
import com.mechuragi.mechuragi_server.global.exception.BusinessException;
import com.mechuragi.mechuragi_server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiServiceClient {

    @Value("${ai-service.url")
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

            if (response == null) {
                log.error("AI 서비스로부터 null 응답 수신");
                throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
            }

            log.info("AI 서비스 응답 수신: {} 개 추천",
                response.getRecommendations() != null ?
                response.getRecommendations().size() : 0);

            return response;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 서비스 호출 실패", e);
            e.printStackTrace();
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }
    }
}