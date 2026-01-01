package com.mechuragi.mechuragi_server.domain.recommend.entity.type;

public enum RecommendationType {
    WEATHER("날씨 기반 추천"),
    TIME_BASED("시간 기반 추천"),
    INGREDIENTS("재료 기반 추천"),
    FEELING("기분 기반 추천"),
    CONVERSATION("대화 기반 추천");

    private final String description;

    RecommendationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
