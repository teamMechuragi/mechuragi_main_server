package com.mechuragi.mechuragi_server.domain.ai.dto.internal.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TimeRecommendationRequest {
    @NotBlank(message = "시간대는 필수입니다")
    private String timeOfDay;
}
