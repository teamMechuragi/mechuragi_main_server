package com.mechuragi.mechuragi_server.domain.vote.dto;

import com.mechuragi.mechuragi_server.domain.vote.entity.VotePost;
import com.mechuragi.mechuragi_server.domain.vote.entity.VotePost.VoteStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoteResponseDTO {

    private Long id;
    private String title;
    private String description;
    private Instant deadline;
    private VoteStatus status;
    private Boolean allowMultipleChoice;
    private int totalParticipants;
    private int totalLikes;
    private Long authorId;
    private String authorName;
    private LocalDateTime createdAt;
    private List<VoteOptionResponseDTO> options;

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
                    return VoteOptionResponseDTO.builder()
                            .id(opt.getId())
                            .optionText(opt.getOptionText())
                            .imageUrl(opt.getImageUrl())
                            .voteCount(count)
                            .votePercentage(opt.getVotePercentage())
                            .displayOrder(opt.getDisplayOrder())
                            .build();
                })
                .collect(Collectors.toList());

        return VoteResponseDTO.builder()
                .id(votePost.getId())
                .title(votePost.getTitle())
                .description(votePost.getDescription())
                .deadline(votePost.getDeadline())
                .status(votePost.getStatus())
                .allowMultipleChoice(votePost.getAllowMultipleChoice())
                .totalParticipants(participants)
                .totalLikes(likes)
                .authorId(votePost.getAuthor().getId())
                .authorName(votePost.getAuthor().getNickname())
                .createdAt(votePost.getCreatedAt())
                .options(options)
                .build();
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VoteOptionResponseDTO {

        private Long id;
        private String optionText;
        private String imageUrl;
        private int voteCount;
        private double votePercentage;
        private int displayOrder;
    }
}