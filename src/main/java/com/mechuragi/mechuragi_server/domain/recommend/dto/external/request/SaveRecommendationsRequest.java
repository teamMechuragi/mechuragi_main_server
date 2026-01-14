package com.mechuragi.mechuragi_server.domain.recommend.dto.external.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveRecommendationsRequest {

    private List<String> context;

    private FoodPreferenceRequest preference;

    @NotEmpty
    @Valid
    private List<SaveRecommendationRequest> recommendations;
}
