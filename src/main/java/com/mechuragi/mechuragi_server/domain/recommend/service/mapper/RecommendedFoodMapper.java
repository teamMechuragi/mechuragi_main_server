package com.mechuragi.mechuragi_server.domain.recommend.service.mapper;

import com.mechuragi.mechuragi_server.domain.member.entity.Member;
import com.mechuragi.mechuragi_server.domain.recommend.dto.common.response.RecommendedFoodResponse;
import com.mechuragi.mechuragi_server.domain.recommend.dto.external.request.FoodPreferenceRequest;
import com.mechuragi.mechuragi_server.domain.recommend.dto.external.request.SaveRecommendationRequest;
import com.mechuragi.mechuragi_server.domain.recommend.dto.external.request.SaveRecommendationsRequest;
import com.mechuragi.mechuragi_server.domain.recommend.entity.RecommendationSession;
import com.mechuragi.mechuragi_server.domain.recommend.entity.RecommendedFood;
import org.springframework.stereotype.Component;

import java.util.List;

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
                .description(request.getDescription())
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
                .description(food.getDescription())
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
                .context(convertListToString(request.getContext()))
                .dietStatus(pref != null ? pref.getDietStatus() : null)
                .veganOption(pref != null ? pref.getVeganOption() : null)
                .spiceLevel(pref != null ? pref.getSpiceLevel() : null)
                .foodTypes(pref != null ? convertListToString(pref.getFoodTypes()) : null)
                .tastes(pref != null ? convertListToString(pref.getTastes()) : null)
                .dislikedFoods(pref != null ? convertListToString(pref.getDislikedFoods()) : null)
                .build();
    }

    private String convertListToString(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return String.join(",", list);
    }
}
