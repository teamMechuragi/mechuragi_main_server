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

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vote_posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class VotePost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member author;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VoteType voteType = VoteType.TEXT;

    @Column(length = 500)
    private String imageUrl;

    @Column(nullable = false)
    private Instant deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VoteStatus status = VoteStatus.ACTIVE;

    @Column(nullable = false)
    private Boolean allowMultipleChoice = false;

    @Column(nullable = false)
    private Boolean isAnonymous = false;

    @Column(nullable = false)
    private Boolean notified10MinBefore = false;

    @OneToMany(mappedBy = "votePost", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VoteOption> voteOptions = new ArrayList<>();

    @OneToMany(mappedBy = "votePost", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VoteParticipation> participations = new ArrayList<>();

    @OneToMany(mappedBy = "votePost", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VoteComment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "votePost", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VoteLike> likes = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public enum VoteType {
        TEXT, IMAGE
    }

    public enum VoteStatus {
        ACTIVE, COMPLETED
    }

    @Builder
    public VotePost(Member author, String title, String description, VoteType voteType,
                   String imageUrl, Instant deadline, Boolean allowMultipleChoice,
                   Boolean isAnonymous) {
        this.author = author;
        this.title = title;
        this.description = description;
        this.voteType = voteType != null ? voteType : VoteType.TEXT;
        this.imageUrl = imageUrl;
        this.deadline = deadline;
        this.allowMultipleChoice = allowMultipleChoice != null ? allowMultipleChoice : false;
        this.isAnonymous = isAnonymous != null ? isAnonymous : false;
        this.status = VoteStatus.ACTIVE;
    }

    public void updatePost(String title, String description, Instant deadline,
                          Boolean allowMultipleChoice, Boolean isAnonymous) {
        if (title != null) this.title = title;
        if (description != null) this.description = description;
        if (deadline != null) this.deadline = deadline;
        if (allowMultipleChoice != null) this.allowMultipleChoice = allowMultipleChoice;
        if (isAnonymous != null) this.isAnonymous = isAnonymous;
    }

    public void updateImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        this.voteType = imageUrl != null ? VoteType.IMAGE : VoteType.TEXT;
    }

    public void changeStatus(VoteStatus status) {
        this.status = status;
    }

    public void completeVote() {
        this.status = VoteStatus.COMPLETED;
    }

    public void updateVote(String title, String description, Instant deadline) {
        if (title != null) this.title = title;
        if (description != null) this.description = description;
        if (deadline != null) this.deadline = deadline;
    }

    public void complete() {
        this.status = VoteStatus.COMPLETED;
    }

    public void markNotified10MinBefore() {
        this.notified10MinBefore = true;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(deadline);
    }

    public boolean isActive() {
        return status == VoteStatus.ACTIVE && !isExpired();
    }

    public int getTotalParticipants() {
        return participations.size();
    }

    public int getTotalLikes() {
        return likes.size();
    }

    public int getTotalComments() {
        return comments.size();
    }
}