package com.mechuragi.mechuragi_server.domain.vote.service;

import com.mechuragi.mechuragi_server.domain.member.entity.Member;
import com.mechuragi.mechuragi_server.domain.member.repository.MemberRepository;
import com.mechuragi.mechuragi_server.domain.vote.dto.VoteCommentCreateRequestDTO;
import com.mechuragi.mechuragi_server.domain.vote.dto.VoteCommentResponseDTO;
import com.mechuragi.mechuragi_server.domain.vote.dto.VoteCommentUpdateRequestDTO;
import com.mechuragi.mechuragi_server.domain.vote.entity.VoteComment;
import com.mechuragi.mechuragi_server.domain.vote.entity.VotePost;
import com.mechuragi.mechuragi_server.domain.vote.repository.VoteCommentRepository;
import com.mechuragi.mechuragi_server.domain.vote.repository.VotePostRepository;
import com.mechuragi.mechuragi_server.global.exception.BusinessException;
import com.mechuragi.mechuragi_server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteCommentService {

    private final VoteCommentRepository voteCommentRepository;
    private final VotePostRepository votePostRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public VoteCommentResponseDTO createComment(Long memberId, VoteCommentCreateRequestDTO request) {
        // 사용자 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 투표 조회
        VotePost votePost = votePostRepository.findById(request.getVoteId())
                .orElseThrow(() -> new BusinessException(ErrorCode.VOTE_NOT_FOUND));

        // 댓글 생성
        VoteComment comment = VoteComment.builder()
                .author(member)
                .votePost(votePost)
                .content(request.getContent())
                .build();

        VoteComment savedComment = voteCommentRepository.save(comment);

        log.info("Comment created: id={}, voteId={}, authorId={}",
                savedComment.getId(), request.getVoteId(), memberId);

        return VoteCommentResponseDTO.from(savedComment);
    }

    public Page<VoteCommentResponseDTO> getComments(Long voteId, Pageable pageable) {
        // 투표 존재 확인
        if (!votePostRepository.existsById(voteId)) {
            throw new BusinessException(ErrorCode.VOTE_NOT_FOUND);
        }

        Page<VoteComment> comments = voteCommentRepository.findByVotePostIdOrderByCreatedAtDesc(voteId, pageable);
        return comments.map(VoteCommentResponseDTO::from);
    }

    @Transactional
    public VoteCommentResponseDTO updateComment(Long commentId, Long memberId, VoteCommentUpdateRequestDTO request) {
        // 댓글 조회 및 권한 확인
        VoteComment comment = voteCommentRepository.findByIdAndAuthorId(commentId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOTE_COMMENT_NOT_FOUND));

        // 댓글 수정
        comment.updateContent(request.getContent());

        log.info("Comment updated: id={}, authorId={}", commentId, memberId);

        return VoteCommentResponseDTO.from(comment);
    }

    @Transactional
    public void deleteComment(Long commentId, Long memberId) {
        // 댓글 조회 및 권한 확인
        VoteComment comment = voteCommentRepository.findByIdAndAuthorId(commentId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOTE_COMMENT_NOT_FOUND));

        voteCommentRepository.delete(comment);

        log.info("Comment deleted: id={}, authorId={}", commentId, memberId);
    }

    public int getCommentCount(Long voteId) {
        return voteCommentRepository.countByVotePostId(voteId);
    }
}
