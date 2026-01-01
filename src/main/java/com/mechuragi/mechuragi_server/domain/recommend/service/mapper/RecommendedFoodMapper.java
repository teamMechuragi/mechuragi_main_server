package com.mechuragi.mechuragi_server.domain.recommend.service.mapper;

import com.mechuragi.mechuragi_server.domain.member.entity.Member;
import com.mechuragi.mechuragi_server.domain.recommend.dto.common.response.RecommendedFoodResponse;
import com.mechuragi.mechuragi_server.domain.recommend.dto.external.request.SaveRecommendedFoodRequest;
import com.mechuragi.mechuragi_server.domain.recommend.entity.RecommendedFood;
import org.springframework.stereotype.Component;

@Component
public class RecommendedFoodMapper {

    /**
     * SaveRecommendedFoodRequest를 RecommendedFood 엔티티로 변환
     */
    public RecommendedFood toEntity(SaveRecommendedFoodRequest request, Member member) {
        return RecommendedFood.builder()
                .member(member)
                .recommendationType(request.getRecommendationType())
                .name(request.getName())
                .description(request.getDescription())
                .reason(request.getReason())
                .ingredients(request.getIngredients())
                .cookingTime(request.getCookingTime())
                .difficulty(request.getDifficulty())
                .isScrapped(false)
                .build();
    }

    /**
     * RecommendedFood 엔티티를 RecommendedFoodResponse DTO로 변환
     */
    public RecommendedFoodResponse toDto(RecommendedFood food) {
        return RecommendedFoodResponse.builder()
                .id(food.getId())
                .recommendationType(food.getRecommendationType())
                .name(food.getName())
                .description(food.getDescription())
                .reason(food.getReason())
                .ingredients(food.getIngredients())
                .cookingTime(food.getCookingTime())
                .difficulty(food.getDifficulty())
                .isScrapped(food.getIsScrapped())
                .createdAt(food.getCreatedAt())
                .build();
    }
}
