package com.mechuragi.mechuragi_server.domain.recommend.dto.external.request;

import com.mechuragi.mechuragi_server.domain.recommend.entity.type.RecommendationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveRecommendedFoodRequest {

    @NotNull
    private Long memberId;

    @NotNull(message = "추천 타입은 필수입니다")
    private RecommendationType recommendationType;

    @NotBlank(message = "음식 이름은 필수입니다")
    private String name;

    private String description;
    private String reason;
    private String ingredients;
    private String cookingTime;
    private String difficulty;
}
