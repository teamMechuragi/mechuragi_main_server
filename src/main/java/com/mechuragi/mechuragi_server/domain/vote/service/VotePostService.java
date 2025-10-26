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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VotePostService {

    private final VotePostRepository votePostRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public VoteResponseDTO createVote(Long authorId, VoteCreateRequestDTO request) {
        Member author = memberRepository.findById(authorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        VotePost votePost = VotePost.builder()
                .title(request.title())
                .description(request.description())
                .deadline(request.deadline())
                .author(author)
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

        return VoteResponseDTO.from(savedVotePost);
    }

    public VoteResponseDTO getVote(Long voteId) {
        VotePost votePost = votePostRepository.findById(voteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOTE_NOT_FOUND));

        return VoteResponseDTO.from(votePost);
    }

    public Page<VoteResponseDTO> getActiveVotes(Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        Page<VotePost> votePosts = votePostRepository.findActiveVotes(now, pageable);
        return votePosts.map(VoteResponseDTO::from);
    }

    public Page<VoteResponseDTO> getCompletedVotes(Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        Page<VotePost> votePosts = votePostRepository.findCompletedVotes(now, pageable);
        return votePosts.map(VoteResponseDTO::from);
    }

    public Page<VoteResponseDTO> getUserVotes(Long userId, Pageable pageable) {
        Page<VotePost> votePosts = votePostRepository.findByAuthorIdOrderByCreatedAtDesc(userId, pageable);
        return votePosts.map(VoteResponseDTO::from);
    }

    @Transactional
    public VoteResponseDTO updateVote(Long voteId, Long authorId, VoteUpdateRequestDTO request) {
        VotePost votePost = votePostRepository.findByIdAndAuthorId(voteId, authorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOTE_NOT_FOUND));

        if (votePost.getStatus() == VoteStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.VOTE_ALREADY_COMPLETED);
        }

        votePost.updateVote(request.title(), request.description(), request.deadline());
        return VoteResponseDTO.from(votePost);
    }

    @Transactional
    public void deleteVote(Long voteId, Long authorId) {
        VotePost votePost = votePostRepository.findByIdAndAuthorId(voteId, authorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOTE_NOT_FOUND));

        votePostRepository.delete(votePost);
    }

    // 스케줄러 호출용 - deadline 지난 투표 COMPLETED로 변경
    @Transactional
    public void completeExpiredVotes() {
        LocalDateTime now = LocalDateTime.now();
        List<VotePost> expiredVotes = votePostRepository.findExpiredActiveVotes(now);

        expiredVotes.forEach(VotePost::complete);
    }
}