package com.mechuragi.mechuragi_server.domain.vote.dto;

import com.mechuragi.mechuragi_server.domain.vote.entity.VoteOption;
import com.mechuragi.mechuragi_server.domain.vote.entity.VotePost;
import com.mechuragi.mechuragi_server.domain.vote.entity.VotePost.VoteStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

        List<VoteOption> voteOptions = votePost.getVoteOptions();
        int n = voteOptions.size();
        int[] counts = new int[n];
        for (int i = 0; i < n; i++) {
            VoteOption opt = voteOptions.get(i);
            String optionKey = "vote:" + votePost.getId() + ":option:" + opt.getId() + ":count";
            counts[i] = Optional.ofNullable(redisTemplate.opsForValue().get(optionKey))
                    .map(Integer::parseInt).orElse(opt.getVoteCount());
        }

        int[] percentages = largestRemainder(counts, participants);

        List<VoteOptionResponseDTO> options = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            VoteOption opt = voteOptions.get(i);
            options.add(VoteOptionResponseDTO.builder()
                    .id(opt.getId())
                    .optionText(opt.getOptionText())
                    .imageUrl(opt.getImageUrl())
                    .voteCount(counts[i])
                    .votePercentage(percentages[i])
                    .displayOrder(opt.getDisplayOrder())
                    .build());
        }

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

    private static int[] largestRemainder(int[] counts, int total) {
        int n = counts.length;
        int[] result = new int[n];
        if (total == 0 || n == 0) return result;

        double[] remainders = new double[n];
        int floorSum = 0;
        for (int i = 0; i < n; i++) {
            double raw = (double) counts[i] / total * 100.0;
            result[i] = (int) raw;
            remainders[i] = raw - result[i];
            floorSum += result[i];
        }

        int remaining = 100 - floorSum;
        Integer[] indices = IntStream.range(0, n).boxed()
                .sorted((a, b) -> Double.compare(remainders[b], remainders[a]))
                .toArray(Integer[]::new);

        for (int i = 0; i < remaining && i < n; i++) {
            result[indices[i]]++;
        }

        return result;
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
        private int votePercentage;
        private int displayOrder;
    }
}