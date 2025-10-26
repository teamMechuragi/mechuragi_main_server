package com.mechuragi.mechuragi_server.domain.vote.dto;

import com.mechuragi.mechuragi_server.domain.vote.entity.VotePost;
import com.mechuragi.mechuragi_server.domain.vote.entity.VotePost.VoteStatus;

import java.time.LocalDateTime;
import java.util.List;

public record VoteResponseDTO(
        Long id,
        String title,
        String description,
        LocalDateTime deadline,
        VoteStatus status,
        Boolean allowMultipleChoice,
        int totalParticipants,
        int totalLikes,
        String authorName,
        LocalDateTime createdAt,
        List<VoteOptionResponseDTO> options
) {
    public static VoteResponseDTO from(VotePost votePost) {
        return new VoteResponseDTO(
                votePost.getId(),
                votePost.getTitle(),
                votePost.getDescription(),
                votePost.getDeadline(),
                votePost.getStatus(),
                votePost.getAllowMultipleChoice(),
                votePost.getTotalParticipants(),
                votePost.getTotalLikes(),
                votePost.getAuthor().getNickname(),
                votePost.getCreatedAt(),
                votePost.getVoteOptions().stream()
                        .map(VoteOptionResponseDTO::from)
                        .toList()
        );
    }

    public record VoteOptionResponseDTO(
            Long id,
            String optionText,
            String imageUrl,
            int voteCount,
            double votePercentage,
            int displayOrder
    ) {
        public static VoteOptionResponseDTO from(com.mechuragi.mechuragi_server.domain.vote.entity.VoteOption voteOption) {
            return new VoteOptionResponseDTO(
                    voteOption.getId(),
                    voteOption.getOptionText(),
                    voteOption.getImageUrl(),
                    voteOption.getVoteCount(),
                    voteOption.getVotePercentage(),
                    voteOption.getDisplayOrder()
            );
        }
    }
}