package com.mechuragi.mechuragi_server.domain.preference.dto;

import lombok.Getter;

@Getter
public class PreferenceListResponse {

    private final Long id;
    private final String preferenceName;
    private final Boolean isActive;

    public PreferenceListResponse(Long id, String preferenceName, Boolean isActive) {
        this.id = id;
        this.preferenceName = preferenceName;
        this.isActive = isActive;
    }
}