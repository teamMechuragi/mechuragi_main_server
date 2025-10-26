package com.mechuragi.mechuragi_server.domain.vote.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vote_options")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class VoteOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vote_post_id", nullable = false)
    private VotePost votePost;

    @Column(nullable = false, length = 100)
    private String optionText;

    @Column(length = 500)
    private String imageUrl;

    @Column(nullable = false)
    private Integer displayOrder = 0;

    @OneToMany(mappedBy = "voteOption", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VoteParticipation> participations = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public VoteOption(VotePost votePost, String optionText, String imageUrl, Integer displayOrder) {
        this.votePost = votePost;
        this.optionText = optionText;
        this.imageUrl = imageUrl;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
    }

    public void updateOption(String optionText, String imageUrl) {
        if (optionText != null) this.optionText = optionText;
        if (imageUrl != null) this.imageUrl = imageUrl;
    }

    public void updateDisplayOrder(Integer displayOrder) {
        if (displayOrder != null) this.displayOrder = displayOrder;
    }

    public int getVoteCount() {
        return participations.size();
    }

    public double getVotePercentage(int totalVotes) {
        if (totalVotes == 0) return 0.0;
        return (double) getVoteCount() / totalVotes * 100.0;
    }

    public double getVotePercentage() {
        if (votePost == null) return 0.0;
        int totalVotes = votePost.getTotalParticipants();
        return getVotePercentage(totalVotes);
    }
}