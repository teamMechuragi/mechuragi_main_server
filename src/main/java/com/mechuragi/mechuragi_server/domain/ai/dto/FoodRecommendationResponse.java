package com.mechuragi.mechuragi_server.domain.ai.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoodRecommendationResponse {

    private String message;
    private List<FoodRecommendation> recommendations;
    private String model;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FoodRecommendation {
        private String name;
        private String description;
        private String reason;
        private String ingredients;
        private String cookingTime;
        private String difficulty;
    }
}