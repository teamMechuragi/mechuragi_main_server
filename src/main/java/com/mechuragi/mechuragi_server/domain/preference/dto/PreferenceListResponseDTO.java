package com.mechuragi.mechuragi_server.domain.preference.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreferenceListResponseDTO {

    private Long id;
    private String preferenceName;
    private Boolean isActive;
}