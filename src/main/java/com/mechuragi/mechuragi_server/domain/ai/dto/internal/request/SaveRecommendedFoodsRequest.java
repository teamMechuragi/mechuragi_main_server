package com.mechuragi.mechuragi_server.domain.ai.dto.internal.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
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
public class SaveRecommendedFoodsRequest {

    @NotNull
    private Long memberId;

    @NotEmpty
    @Valid
    private List<SaveRecommendedFoodRequest> recommendations;
}
