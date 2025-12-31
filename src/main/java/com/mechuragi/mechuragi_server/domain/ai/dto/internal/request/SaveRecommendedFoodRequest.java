package com.mechuragi.mechuragi_server.domain.ai.dto.internal.request;

import com.mechuragi.mechuragi_server.domain.ai.entity.type.RecommendationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveRecommendedFoodRequest {

    @NotNull
    private Long memberId;

    @NotNull
    private RecommendationType recommendationType;

    @NotBlank
    private String name;

    private String description;
    private String reason;
    private String ingredients;
    private String cookingTime;
    private String difficulty;
}
