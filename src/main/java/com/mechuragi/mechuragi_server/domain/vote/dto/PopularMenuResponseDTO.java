package com.mechuragi.mechuragi_server.domain.vote.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PopularMenuResponseDTO {

    private String menu;                    // 메뉴명
    private double score;                   // 최종 점수
    private int mentionCount;               // 언급 횟수
    private double averageVotePercentage;   // 평균 투표율 (실시간)
    private double averageRecency;          // 평균 최근성
}
