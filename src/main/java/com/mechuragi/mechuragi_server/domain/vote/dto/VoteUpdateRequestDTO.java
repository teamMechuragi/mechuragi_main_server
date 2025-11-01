package com.mechuragi.mechuragi_server.domain.vote.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoteUpdateRequestDTO {

    @NotBlank(message = "투표 제목은 필수입니다")
    @Size(max = 50, message = "투표 제목은 50자 이하여야 합니다")
    private String title;

    @Size(max = 50, message = "투표 설명은 50자 이하여야 합니다")
    private String description;

    @NotNull(message = "마감일은 필수입니다")
    private LocalDateTime deadline;
}