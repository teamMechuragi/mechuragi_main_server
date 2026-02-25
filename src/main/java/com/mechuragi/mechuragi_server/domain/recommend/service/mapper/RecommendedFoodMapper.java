package com.mechuragi.mechuragi_server.domain.recommend.service.mapper;

import com.mechuragi.mechuragi_server.domain.member.entity.Member;
import com.mechuragi.mechuragi_server.domain.recommend.dto.common.response.RecommendedFoodResponse;
import com.mechuragi.mechuragi_server.domain.recommend.dto.external.request.FoodPreferenceRequest;
import com.mechuragi.mechuragi_server.domain.recommend.dto.external.request.SaveRecommendationRequest;
import com.mechuragi.mechuragi_server.domain.recommend.dto.external.request.SaveRecommendationsRequest;
import com.mechuragi.mechuragi_server.domain.recommend.entity.RecommendationSession;
import com.mechuragi.mechuragi_server.domain.recommend.entity.RecommendedFood;
import org.springframework.stereotype.Component;

@Component
public class RecommendedFoodMapper {

    /**
     * SaveRecommendationRequest를 RecommendedFood 엔티티로 변환
     */
    public RecommendedFood toEntity(SaveRecommendationRequest request, Member member) {
        return RecommendedFood.builder()
                .member(member)
                .recommendationType(request.getRecommendationType())
                .name(request.getName())
                .reason(request.getReason())
                .build();
    }

    /**
     * RecommendedFood 엔티티를 RecommendedFoodResponse DTO로 변환
     */
    public RecommendedFoodResponse toDto(RecommendedFood food) {
        return toDto(food, false);
    }

    /**
     * RecommendedFood 엔티티를 RecommendedFoodResponse DTO로 변환 (북마크 상태 포함)
     */
    public RecommendedFoodResponse toDto(RecommendedFood food, boolean isScrapped) {
        return RecommendedFoodResponse.builder()
                .id(food.getId())
                .recommendationType(food.getRecommendationType())
                .name(food.getName())
                .reason(food.getReason())
                .isScrapped(isScrapped)
                .createdAt(food.getCreatedAt())
                .build();
    }

    /**
     * SaveRecommendationsRequest를 RecommendationSession 엔티티로 변환
     */
    public RecommendationSession toSessionEntity(SaveRecommendationsRequest request, Member member) {
        FoodPreferenceRequest pref = request.getPreference();

        return RecommendationSession.builder()
                .member(member)
                .context(request.getContext())
                .numberOfDiners(pref != null ? pref.getNumberOfDiners() : null)
                .dietStatus(pref != null ? pref.getDietStatus() : null)
                .veganOption(pref != null ? pref.getVeganOption() : null)
                .spiceLevel(pref != null ? pref.getSpiceLevel() : null)
                .preferredFoodTypes(pref != null ? pref.getFoodTypes() : null)
                .preferredTastes(pref != null ? pref.getTastes() : null)
                .avoidedFoods(pref != null ? pref.getAvoidedFoods() : null)
                .allergies(pref != null ? pref.getAllergies() : null)
                .build();
    }
}
