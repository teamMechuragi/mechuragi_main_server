package com.mechuragi.mechuragi_server.domain.ai.service.mapper;

import com.mechuragi.mechuragi_server.domain.ai.dto.internal.request.ScrapeFoodRequest;
import com.mechuragi.mechuragi_server.domain.ai.dto.common.response.ScrapedFoodResponse;
import com.mechuragi.mechuragi_server.domain.ai.entity.ScrapedFood;
import com.mechuragi.mechuragi_server.domain.member.entity.Member;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ScrapFoodMapper {

    /**
     * ScrapeFoodRequest -> ScrapedFood 엔티티로 변환 (빌더 패턴 사용)
     */
    public ScrapedFood toEntity(ScrapeFoodRequest request, Member member) {
        return ScrapedFood.builder()
                .member(member)
                .recommendationType(request.getRecommendationType())
                .name(request.getName())
                .description(request.getDescription())
                .reason(request.getReason())
                .ingredients(request.getIngredients())
                .cookingTime(request.getCookingTime())
                .difficulty(request.getDifficulty())
                .build();
    }

    /**
     * ScrapedFood 엔티티 ->  ScrapedFoodResponse로 변환
     */
    public ScrapedFoodResponse toResponse(ScrapedFood scrapedFood) {
        return ScrapedFoodResponse.builder()
                .id(scrapedFood.getId())
                .recommendationType(scrapedFood.getRecommendationType())
                .name(scrapedFood.getName())
                .description(scrapedFood.getDescription())
                .reason(scrapedFood.getReason())
                .ingredients(scrapedFood.getIngredients())
                .cookingTime(scrapedFood.getCookingTime())
                .difficulty(scrapedFood.getDifficulty())
                .createdAt(scrapedFood.getCreatedAt())
                .build();
    }

    /**
     * ScrapedFood 엔티티 리스트 -> ScrapedFoodResponse 리스트로 변환
     */
    public List<ScrapedFoodResponse> toResponseList(List<ScrapedFood> scrapedFoods) {
        return scrapedFoods.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}
