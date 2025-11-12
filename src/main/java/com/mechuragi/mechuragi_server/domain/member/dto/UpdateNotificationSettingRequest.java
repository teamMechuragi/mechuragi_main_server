package com.mechuragi.mechuragi_server.domain.member.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateNotificationSettingRequest {

    @NotNull(message = "알림 설정 값은 필수입니다.")
    private Boolean enabled;
}
