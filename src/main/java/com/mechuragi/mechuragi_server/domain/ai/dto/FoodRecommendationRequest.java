package com.mechuragi.mechuragi_server.domain.ai.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoodRecommendationRequest {

    private RecommendationType type;
    private UserPreference userPreference;
    private ContextInfo context;
    private String userMessage;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserPreference {
        private String dietStatus;
        private String veganOption;
        private String spiceLevel;
        private List<String> foodTypes;
        private List<String> tastes;
        private List<String> dislikedFoods;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContextInfo {
        private List<String> weatherConditions;
        private String timeOfDay;
    }

    public enum RecommendationType {
        WEATHER,
        TIME_BASED,
        CONVERSATION
    }
}