package com.mechuragi.mechuragi_server.domain.vote.service;

import com.mechuragi.mechuragi_server.domain.member.entity.Member;
import com.mechuragi.mechuragi_server.domain.member.repository.MemberRepository;
import com.mechuragi.mechuragi_server.domain.vote.dto.VoteCreateRequestDTO;
import com.mechuragi.mechuragi_server.domain.vote.dto.VoteResponseDTO;
import com.mechuragi.mechuragi_server.domain.vote.dto.VoteUpdateRequestDTO;
import com.mechuragi.mechuragi_server.domain.vote.entity.VoteOption;
import com.mechuragi.mechuragi_server.domain.vote.entity.VotePost;
import com.mechuragi.mechuragi_server.domain.vote.entity.VotePost.VoteStatus;
import com.mechuragi.mechuragi_server.domain.vote.repository.VotePostRepository;
import com.mechuragi.mechuragi_server.global.exception.BusinessException;
import com.mechuragi.mechuragi_server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VotePostService {

    private final VotePostRepository votePostRepository;
    private final MemberRepository memberRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional
    public VoteResponseDTO createVote(Long authorId, VoteCreateRequestDTO request) {
        Member author = memberRepository.findById(authorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        boolean hasImage = request.options().stream()
                .anyMatch(option -> option.imageUrl() != null && !option.imageUrl().trim().isEmpty());

        VotePost votePost = VotePost.builder()
                .title(request.title())
                .description(request.description())
                .deadline(request.deadline())
                .allowMultipleChoice(request.allowMultipleChoice())
                .author(author)
                .voteType(hasImage ? VotePost.VoteType.IMAGE : VotePost.VoteType.TEXT)
                .build();

        List<VoteOption> voteOptions = new ArrayList<>();
        for (int i = 0; i < request.options().size(); i++) {
            VoteCreateRequestDTO.VoteOptionRequestDTO option = request.options().get(i);
            voteOptions.add(VoteOption.builder()
                    .optionText(option.optionText())
                    .imageUrl(option.imageUrl())
                    .displayOrder(i + 1)
                    .votePost(votePost)
                    .build());
        }

        votePost.getVoteOptions().addAll(voteOptions);
        VotePost savedVotePost = votePostRepository.save(votePost);

        return VoteResponseDTO.from(savedVotePost, redisTemplate);
    }

    public VoteResponseDTO getVote(Long voteId) {
        VotePost votePost = votePostRepository.findById(voteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOTE_NOT_FOUND));

        return VoteResponseDTO.from(votePost, redisTemplate);
    }

    public Page<VoteResponseDTO> getActiveVotes(Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        Page<VotePost> votePosts = votePostRepository.findActiveVotes(now, pageable);
        return votePosts.map(v -> VoteResponseDTO.from(v, redisTemplate));
    }

    public Page<VoteResponseDTO> getCompletedVotes(Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        Page<VotePost> votePosts = votePostRepository.findCompletedVotes(now, pageable);
        return votePosts.map(v -> VoteResponseDTO.from(v, redisTemplate));
    }

    public Page<VoteResponseDTO> getUserVotes(Long userId, Pageable pageable) {
        Page<VotePost> votePosts = votePostRepository.findByAuthorIdOrderByCreatedAtDesc(userId, pageable);
        return votePosts.map(v -> VoteResponseDTO.from(v, redisTemplate));
    }

    @Cacheable(value = "hotVotes", key = "'list:' + #size")
    public List<VoteResponseDTO> getHotVotes(int size) {
        // Redis Sorted Set에서 상위 N개의 투표 ID 조회 (점수 높은 순)
        Set<String> topVoteIds = redisTemplate.opsForZSet()
                .reverseRange("vote:hot", 0, size - 1);

        if (topVoteIds == null || topVoteIds.isEmpty()) {
            return new ArrayList<>();
        }

        // VotePost 조회
        List<Long> voteIds = topVoteIds.stream()
                .map(Long::valueOf)
                .collect(Collectors.toList());

        List<VotePost> hotVotes = votePostRepository.findAllById(voteIds);

        // Redis에서 가져온 순서대로 정렬
        Map<Long, VotePost> voteMap = hotVotes.stream()
                .collect(Collectors.toMap(VotePost::getId, v -> v));

        return voteIds.stream()
                .map(voteMap::get)
                .filter(Objects::nonNull)
                .map(v -> VoteResponseDTO.from(v, redisTemplate))
                .collect(Collectors.toList());
    }

    @Transactional
    public VoteResponseDTO updateVote(Long voteId, Long authorId, VoteUpdateRequestDTO request) {
        VotePost votePost = votePostRepository.findByIdAndAuthorId(voteId, authorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOTE_NOT_FOUND));

        if (votePost.getStatus() == VoteStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.VOTE_ALREADY_COMPLETED);
        }

        votePost.updateVote(request.title(), request.description(), request.deadline());
        return VoteResponseDTO.from(votePost, redisTemplate);
    }

    @Transactional
    public void deleteVote(Long voteId, Long authorId) {
        VotePost votePost = votePostRepository.findByIdAndAuthorId(voteId, authorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOTE_NOT_FOUND));

        votePostRepository.delete(votePost);
    }

    @Transactional
    public void completeExpiredVotes() {
        LocalDateTime now = LocalDateTime.now();
        List<VotePost> expiredVotes = votePostRepository.findExpiredActiveVotes(now);
        expiredVotes.forEach(VotePost::complete);
    }
}