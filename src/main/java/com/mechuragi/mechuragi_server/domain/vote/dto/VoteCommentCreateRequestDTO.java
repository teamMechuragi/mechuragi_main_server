package com.mechuragi.mechuragi_server.domain.vote.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VoteCommentCreateRequestDTO(
        @NotNull(message = "투표 ID는 필수입니다")
        Long voteId,

        @NotBlank(message = "댓글 내용은 필수입니다")
        @Size(max = 500, message = "댓글은 500자 이하여야 합니다")
        String content
) {
}
