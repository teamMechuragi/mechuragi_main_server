package com.mechuragi.mechuragi_server.domain.vote.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoteCreateRequestDTO {

    @NotBlank(message = "투표 제목은 필수입니다")
    @Size(max = 50, message = "투표 제목은 50자 이하여야 합니다")
    private String title;

    @Size(max = 50, message = "투표 설명은 50자 이하여야 합니다")
    private String description;

    @NotNull(message = "마감일은 필수입니다")
    private LocalDateTime deadline;

    private Boolean allowMultipleChoice;

    @NotEmpty(message = "투표 선택지는 최소 2개 이상이어야 합니다")
    @Size(min = 2, max = 10, message = "투표 선택지는 2개 이상 10개 이하여야 합니다")
    @Valid
    private List<VoteOptionRequestDTO> options;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VoteOptionRequestDTO {

        @NotBlank(message = "선택지 텍스트는 필수입니다")
        @Size(max = 50, message = "선택지 텍스트는 50자 이하여야 합니다")
        private String optionText;

        private String imageUrl;
    }
}