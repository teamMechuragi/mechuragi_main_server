package com.mechuragi.mechuragi_server.domain.recommend.dto.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookmarkedSessionResponse {

    private Long sessionId;
    private List<RecommendedFoodResponse> foods;
    private LocalDateTime createdAt;
}
