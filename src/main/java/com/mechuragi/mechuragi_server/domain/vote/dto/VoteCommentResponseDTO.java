package com.mechuragi.mechuragi_server.domain.vote.dto;

import com.mechuragi.mechuragi_server.domain.vote.entity.VoteComment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoteCommentResponseDTO {

    private Long id;
    private Long voteId;
    private String content;
    private String authorName;
    private Long authorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static VoteCommentResponseDTO from(VoteComment comment) {
        return VoteCommentResponseDTO.builder()
                .id(comment.getId())
                .voteId(comment.getVotePost().getId())
                .content(comment.getContent())
                .authorName(comment.getAuthor().getNickname())
                .authorId(comment.getAuthor().getId())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
