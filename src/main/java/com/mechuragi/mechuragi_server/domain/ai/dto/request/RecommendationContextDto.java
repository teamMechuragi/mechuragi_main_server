package com.mechuragi.mechuragi_server.domain.ai.dto.request;

import lombok.*;

import java.util.List;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationContextDto {
    private List<String> weatherConditions;
    private String timeOfDay;
    private List<String> ingredients;
    private String feeling;
}