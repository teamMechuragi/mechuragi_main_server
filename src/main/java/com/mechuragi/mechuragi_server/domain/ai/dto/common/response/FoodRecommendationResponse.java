package com.mechuragi.mechuragi_server.domain.ai.dto.common.response;

import lombok.*;

import java.util.List;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FoodRecommendationResponse {

    private String message;
    private List<FoodRecommendation> recommendations;
    private String model;

}