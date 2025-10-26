package com.mechuragi.mechuragi_server.domain.vote.dto;

import com.mechuragi.mechuragi_server.domain.vote.entity.VoteParticipation;

import java.time.LocalDateTime;
import java.util.List;

public record VoteParticipationResponseDTO(
        Long voteId,
        String voteTitle,
        List<ParticipatedOptionDTO> participatedOptions,
        LocalDateTime participatedAt
) {
    public record ParticipatedOptionDTO(
            Long optionId,
            String optionText,
            String imageUrl,
            Integer voteCount,
            Double votePercentage
    ) {
    }

    public static VoteParticipationResponseDTO from(Long voteId, String voteTitle,
                                                     List<VoteParticipation> participations) {
        List<ParticipatedOptionDTO> options = participations.stream()
                .map(p -> new ParticipatedOptionDTO(
                        p.getVoteOption().getId(),
                        p.getVoteOption().getOptionText(),
                        p.getVoteOption().getImageUrl(),
                        p.getVoteOption().getVoteCount(),
                        p.getVoteOption().getVotePercentage()
                ))
                .toList();

        LocalDateTime participatedAt = participations.isEmpty()
                ? LocalDateTime.now()
                : participations.get(0).getCreatedAt();

        return new VoteParticipationResponseDTO(
                voteId,
                voteTitle,
                options,
                participatedAt
        );
    }
}
