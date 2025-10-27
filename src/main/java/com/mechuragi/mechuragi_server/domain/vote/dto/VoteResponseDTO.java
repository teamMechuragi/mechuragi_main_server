package com.mechuragi.mechuragi_server.domain.vote.dto;

import com.mechuragi.mechuragi_server.domain.vote.entity.VotePost;
import com.mechuragi.mechuragi_server.domain.vote.entity.VotePost.VoteStatus;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
    public static VoteResponseDTO from(VotePost votePost, RedisTemplate<String, String> redisTemplate) {
        String participantsKey = "vote:" + votePost.getId() + ":participants";
        int participants = Optional.ofNullable(redisTemplate.opsForValue().get(participantsKey))
                .map(Integer::parseInt).orElse(votePost.getTotalParticipants());

        String likesKey = "vote:" + votePost.getId() + ":likes";
        int likes = Optional.ofNullable(redisTemplate.opsForValue().get(likesKey))
                .map(Integer::parseInt).orElse(votePost.getTotalLikes());

        List<VoteOptionResponseDTO> options = votePost.getVoteOptions().stream()
                .map(opt -> {
                    String optionKey = "vote:" + votePost.getId() + ":option:" + opt.getId() + ":count";
                    int count = Optional.ofNullable(redisTemplate.opsForValue().get(optionKey))
                            .map(Integer::parseInt).orElse(opt.getVoteCount());
                    return new VoteOptionResponseDTO(
                            opt.getId(),
                            opt.getOptionText(),
                            opt.getImageUrl(),
                            count,
                            opt.getVotePercentage(),
                            opt.getDisplayOrder()
                    );
                })
                .toList();

        return new VoteResponseDTO(
                votePost.getId(),
                votePost.getTitle(),
                votePost.getDescription(),
                votePost.getDeadline(),
                votePost.getStatus(),
                votePost.getAllowMultipleChoice(),
                participants,
                likes,
                votePost.getAuthor().getNickname(),
                votePost.getCreatedAt(),
                options
        );
    }

    public record VoteOptionResponseDTO(
            Long id,
            String optionText,
            String imageUrl,
            int voteCount,
            double votePercentage,
            int displayOrder
    ) {}
}