package com.mechuragi.mechuragi_server.domain.vote.dto;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 메뉴별 점수 계산 클래스
 */
@Getter
public class MenuScoreDTO {
    private final String menuName;
    private int mentionCount = 0;
    private final List<Double> votePercentages = new ArrayList<>();
    private final List<Double> recencyScores = new ArrayList<>();

    public MenuScoreDTO(String menuName) {
        this.menuName = menuName;
    }

    public void addData(double votePercentage, double recency) {
        mentionCount++;
        votePercentages.add(votePercentage);
        recencyScores.add(recency);
    }

    public double getAverageVotePercentage() {
        return votePercentages.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    public double getAverageRecency() {
        return recencyScores.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    public double getScore() {
        double avgVotePercentage = getAverageVotePercentage();
        double avgRecency = getAverageRecency();

        return (mentionCount * 10.0)
                + (avgVotePercentage * 0.01)
                + (avgRecency * 2.0)
                + 1.0;
    }
}
