package com.mechuragi.mechuragi_server.domain.ai.dto.request;

import lombok.*;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FoodRecommendationRequest {

    private RecommendationType type;
    private FoodPreferenceDto preference;
    private RecommendationContextDto context;
    private String userMessage;

}