package com.mechuragi.mechuragi_server.domain.ai.service.mapper;

import com.mechuragi.mechuragi_server.domain.ai.dto.request.FoodPreferenceDto;
import com.mechuragi.mechuragi_server.domain.ai.dto.request.FoodRecommendationRequest;
import com.mechuragi.mechuragi_server.domain.ai.dto.request.RecommendationContextDto;
import com.mechuragi.mechuragi_server.domain.ai.dto.request.RecommendationType;

import java.util.List;

public class FoodRecommendationMapper {
    /**
     * FoodPreference 생성
     */
    public FoodPreferenceDto toFoodPreferenceDto(
            String dietStatus,
            String veganOption,
            String spiceLevel,
            List<String> foodTypes,
            List<String> tastes,
            List<String> dislikedFoods) {

        return FoodPreferenceDto.builder()
                .dietStatus(dietStatus)
                .veganOption(veganOption)
                .spiceLevel(spiceLevel)
                .foodTypes(foodTypes)
                .tastes(tastes)
                .dislikedFoods(dislikedFoods)
                .build();
    }

    /**
     * RecommendationContext 생성
     */
    public RecommendationContextDto toRecommendationContextDto(
            List<String> weatherConditions,
            String timeOfDay,
            List<String> ingredients,
            String feeling) {

        return RecommendationContextDto.builder()
                .weatherConditions(weatherConditions)
                .timeOfDay(timeOfDay)
                .ingredients(ingredients)
                .feeling(feeling)
                .build();
    }

    /**
     * 날씨 기반 추천 요청 생성
     */
    public FoodRecommendationRequest createWeatherBasedRequest(
            FoodPreferenceDto foodPreferenceDto,
            List<String> weatherConditions) {

        RecommendationContextDto context = RecommendationContextDto.builder()
                .weatherConditions(weatherConditions)
                .build();

        return FoodRecommendationRequest.builder()
                .type(RecommendationType.WEATHER)
                .preference(foodPreferenceDto)
                .context(context)
                .build();
    }

    /**
     * 시간 기반 추천 요청 생성
     */
    public FoodRecommendationRequest createTimeBasedRequest(
            FoodPreferenceDto foodPreferenceDto,
            String timeOfDay) {

        RecommendationContextDto context = RecommendationContextDto.builder()
                .timeOfDay(timeOfDay)
                .build();

        return FoodRecommendationRequest.builder()
                .type(RecommendationType.TIME_BASED)
                .preference(foodPreferenceDto)
                .context(context)
                .build();
    }

    /**
     * 재료 기반 추천 요청 생성
     */
    public FoodRecommendationRequest createIngredientsBasedRequest(
            FoodPreferenceDto foodPreferenceDto,
            List<String> ingredients) {

        RecommendationContextDto context = RecommendationContextDto.builder()
                .ingredients(ingredients)
                .build();

        return FoodRecommendationRequest.builder()
                .type(RecommendationType.INGREDIENTS)
                .preference(foodPreferenceDto)
                .context(context)
                .build();
    }

    /**
     * 기분 기반 추천 요청 생성
     */
    public FoodRecommendationRequest createFeelingBasedRequest(
            FoodPreferenceDto foodPreferenceDto,
            String feeling) {

        RecommendationContextDto context = RecommendationContextDto.builder()
                .feeling(feeling)
                .build();

        return FoodRecommendationRequest.builder()
                .type(RecommendationType.FEELING)
                .preference(foodPreferenceDto)
                .context(context)
                .build();
    }

    /**
     * 대화 기반 추천 요청 생성
     */
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
