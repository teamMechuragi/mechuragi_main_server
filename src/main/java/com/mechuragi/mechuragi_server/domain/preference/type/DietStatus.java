package com.mechuragi.mechuragi_server.domain.preference.type;

public enum DietStatus {
    NONE("해당 없음"),
    WEIGHT_LOSS("다이어트 중"),
    BULKING("근성장"),
    MAINTENANCE("유지어터");

    private final String description;
    DietStatus(String description) { this.description = description; }
    public String getDescription() { return description; }
}
