package com.mechuragi.mechuragi_server.domain.vote.entity;

import com.mechuragi.mechuragi_server.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "vote_comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class VoteComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member author; // 댓글 작성자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vote_post_id", nullable = false)
    private VotePost votePost; // 댓글이 달린 투표 게시물

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content; // 댓글 내용

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public VoteComment(Member author, VotePost votePost, String content) {
        this.author = author;
        this.votePost = votePost;
        this.content = content;
    }

    // 댓글 내용 수정
    public void updateContent(String content) {
        if (content != null) this.content = content;
    }
}