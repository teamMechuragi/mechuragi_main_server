package com.mechuragi.mechuragi_server.domain.ai.dto.external.request;

import com.mechuragi.mechuragi_server.domain.ai.type.RecommendationType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FoodRecommendationRequest {

    @NotNull(message = "추천 타입은 필수입니다")
    private RecommendationType type;

    @Valid
    private FoodPreferenceDto preference;

    // 컨텍스트 필드들
    private List<String> weatherConditions;
    private String timeOfDay;
    private List<String> ingredients;
    private String feeling;
    private String userMessage;

}