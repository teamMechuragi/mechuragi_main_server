package com.mechuragi.mechuragi_server.domain.vote.service.calculator;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 메뉴 점수 계산 유틸리티
 */
@Component
public class MenuScoreCalculator {

    /**
     * 최근성 점수 계산 (7일 기준)
     */
    public double calculateRecencyScore(LocalDateTime createdAt) {
        long daysSinceCreated = ChronoUnit.DAYS.between(createdAt, LocalDateTime.now());
        return Math.max(0, 1.0 - (daysSinceCreated / 7.0));
    }

    /**
     * 최종 메뉴 점수 계산
     * score = (mentionCount * 10.0) + (avgVotePercentage * 0.01) + (avgRecency * 2.0) + 1.0
     */
    public double calculateFinalScore(int mentionCount, double avgVotePercentage, double avgRecency) {
        return (mentionCount * 10.0)
                + (avgVotePercentage * 0.01)
                + (avgRecency * 2.0)
                + 1.0;
    }
}
