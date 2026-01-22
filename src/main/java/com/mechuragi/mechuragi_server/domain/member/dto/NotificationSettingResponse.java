package com.mechuragi.mechuragi_server.domain.member.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NotificationSettingResponse {
    private Boolean enabled;

    public NotificationSettingResponse(Boolean enabled) {
        this.enabled = enabled;
    }

}
