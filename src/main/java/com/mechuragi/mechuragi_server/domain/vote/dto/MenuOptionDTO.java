package com.mechuragi.mechuragi_server.domain.vote.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 메뉴 옵션 데이터 (실시간 투표율 포함)
 */
@Getter
@AllArgsConstructor
public class MenuOptionDTO {
    private String optionText;              // 메뉴명
    private int voteCount;                  // 투표 수 (Redis)
    private double realtimeVotePercentage;  // 실시간 투표율 (Redis 기반 재계산)
    private LocalDateTime createdAt;        // 투표 생성일 (최근성 계산용)
}
