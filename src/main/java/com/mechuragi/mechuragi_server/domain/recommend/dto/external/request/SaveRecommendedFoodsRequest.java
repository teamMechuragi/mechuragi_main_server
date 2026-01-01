package com.mechuragi.mechuragi_server.domain.recommend.dto.external.request;

import com.mechuragi.mechuragi_server.domain.recommend.entity.type.RecommendationType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
// 다수 추천 음식 저장 요청
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveRecommendedFoodsRequest {

    @NotNull
    private Long memberId;

    @NotNull(message = "추천 타입은 필수입니다")
    private RecommendationType recommendationType;

    @NotEmpty
    @Valid
    private List<SaveRecommendedFoodRequest> recommendations;
}
