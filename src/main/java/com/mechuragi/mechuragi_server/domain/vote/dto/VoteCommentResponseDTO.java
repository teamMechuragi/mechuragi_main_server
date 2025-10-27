package com.mechuragi.mechuragi_server.domain.vote.dto;

import com.mechuragi.mechuragi_server.domain.vote.entity.VoteComment;

import java.time.LocalDateTime;

public record VoteCommentResponseDTO(
        Long id,
        Long voteId,
        String content,
        String authorName,
        Long authorId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static VoteCommentResponseDTO from(VoteComment comment) {
        return new VoteCommentResponseDTO(
                comment.getId(),
                comment.getVotePost().getId(),
                comment.getContent(),
                comment.getAuthor().getNickname(),
                comment.getAuthor().getId(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
