package com.mechuragi.mechuragi_server.domain.ai.dto.internal.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class IngredientsRecommendationRequest {
    private List<String> ingredients;
}
