package com.mechuragi.mechuragi_server.domain.vote.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record VoteParticipationRequestDTO(
        @NotNull(message = "투표 ID는 필수입니다")
        Long voteId,

        @NotEmpty(message = "선택한 옵션은 최소 1개 이상이어야 합니다")
        List<Long> optionIds
) {
}
