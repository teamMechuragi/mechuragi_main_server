package com.mechuragi.mechuragi_server.domain.ai.dto.internal.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class IngredientsRecommendationRequest {
    @NotEmpty(message = "재료는 최소 1개 이상 필요합니다")
    private List<String> ingredients;
}
