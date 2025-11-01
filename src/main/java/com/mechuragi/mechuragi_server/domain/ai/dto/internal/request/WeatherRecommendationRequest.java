package com.mechuragi.mechuragi_server.domain.ai.dto.internal.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class WeatherRecommendationRequest {
    @NotEmpty(message = "날씨 조건은 최소 1개 이상 필요합니다")
    private List<String> weatherConditions;
}
