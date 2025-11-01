package com.mechuragi.mechuragi_server.domain.vote.dto;

import com.mechuragi.mechuragi_server.domain.vote.entity.VoteParticipation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoteParticipationResponseDTO {

    private Long voteId;
    private String voteTitle;
    private List<ParticipatedOptionDTO> participatedOptions;
    private LocalDateTime participatedAt;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipatedOptionDTO {

        private Long optionId;
        private String optionText;
        private String imageUrl;
        private Integer voteCount;
        private Double votePercentage;
    }

    public static VoteParticipationResponseDTO from(Long voteId, String voteTitle,
                                                     List<VoteParticipation> participations) {
        List<ParticipatedOptionDTO> options = participations.stream()
                .map(p -> ParticipatedOptionDTO.builder()
                        .optionId(p.getVoteOption().getId())
                        .optionText(p.getVoteOption().getOptionText())
                        .imageUrl(p.getVoteOption().getImageUrl())
                        .voteCount(p.getVoteOption().getVoteCount())
                        .votePercentage(p.getVoteOption().getVotePercentage())
                        .build())
                .collect(Collectors.toList());

        LocalDateTime participatedAt = participations.isEmpty()
                ? LocalDateTime.now()
                : participations.get(0).getCreatedAt();

        return VoteParticipationResponseDTO.builder()
                .voteId(voteId)
                .voteTitle(voteTitle)
                .participatedOptions(options)
                .participatedAt(participatedAt)
                .build();
    }
}
