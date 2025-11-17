package com.mechuragi.mechuragi_server.domain.ai.service.mapper;

import com.mechuragi.mechuragi_server.domain.ai.dto.external.request.FoodPreferenceDto;
import com.mechuragi.mechuragi_server.domain.ai.dto.external.request.FoodRecommendationRequest;
import com.mechuragi.mechuragi_server.domain.ai.entity.type.RecommendationType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FoodRecommendationMapper {

    public FoodRecommendationRequest createWeatherBasedRequest(
            FoodPreferenceDto foodPreferenceDto,
            List<String> weatherConditions) {

        return FoodRecommendationRequest.builder()
                .type(RecommendationType.WEATHER)
                .preference(foodPreferenceDto)
                .weatherConditions(weatherConditions)
                .build();
    }

    public FoodRecommendationRequest createTimeBasedRequest(
            FoodPreferenceDto foodPreferenceDto,
            String timeOfDay) {

        return FoodRecommendationRequest.builder()
                .type(RecommendationType.TIME_BASED)
                .preference(foodPreferenceDto)
                .timeOfDay(timeOfDay)
                .build();
    }

    public FoodRecommendationRequest createIngredientsBasedRequest(
            FoodPreferenceDto foodPreferenceDto,
            List<String> ingredients) {

        return FoodRecommendationRequest.builder()
                .type(RecommendationType.INGREDIENTS)
                .preference(foodPreferenceDto)
                .ingredients(ingredients)
                .build();
    }

    public FoodRecommendationRequest createFeelingBasedRequest(
            FoodPreferenceDto foodPreferenceDto,
            String feeling) {

        return FoodRecommendationRequest.builder()
                .type(RecommendationType.FEELING)
                .preference(foodPreferenceDto)
                .feeling(feeling)
                .build();
    }

    public FoodRecommendationRequest createConversationBasedRequest(
            FoodPreferenceDto foodPreferenceDto,
            String userMessage) {

        return FoodRecommendationRequest.builder()
                .type(RecommendationType.CONVERSATION)
                .preference(foodPreferenceDto)
                .userMessage(userMessage)
                .build();
    }
}
