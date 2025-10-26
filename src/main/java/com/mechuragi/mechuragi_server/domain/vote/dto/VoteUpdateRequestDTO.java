package com.mechuragi.mechuragi_server.domain.vote.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record VoteUpdateRequestDTO(
        @NotBlank(message = "투표 제목은 필수입니다")
        @Size(max = 50, message = "투표 제목은 50자 이하여야 합니다")
        String title,

        @Size(max = 50, message = "투표 설명은 50자 이하여야 합니다")
        String description,

        @NotNull(message = "마감일은 필수입니다")
        LocalDateTime deadline
) {
}