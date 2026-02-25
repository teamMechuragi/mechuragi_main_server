package com.mechuragi.mechuragi_server.domain.preference.type;

public enum VeganOption {
    NONE("해당 없음 (일반식)"),
    VEGAN("비건 (완전 채식)"),
    VEGETARIAN("베지테리언 (유제품/달걀 허용)"),
    PESCATARIAN("페스코 (생선까지 허용)"),
    FLEXITARIAN("플렉시테리언 (간헐적 채식)");

    private final String description;
    VeganOption(String description) { this.description = description; }
    public String getDescription() { return description; }
}
