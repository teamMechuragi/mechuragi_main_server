package com.mechuragi.mechuragi_server.domain.ai.dto.internal.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConversationRecommendationRequest {
    @NotBlank(message = "메시지는 필수입니다")
    private String message;
}
