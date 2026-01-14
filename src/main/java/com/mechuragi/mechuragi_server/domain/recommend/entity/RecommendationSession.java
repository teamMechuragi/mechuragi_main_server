package com.mechuragi.mechuragi_server.domain.recommend.entity;

import com.mechuragi.mechuragi_server.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recommendation_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class RecommendationSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(columnDefinition = "TEXT")
    private String context;

    // Preference fields
    private String dietStatus;
    private String veganOption;
    private String spiceLevel;

    @Column(columnDefinition = "TEXT")
    private String foodTypes;

    @Column(columnDefinition = "TEXT")
    private String tastes;

    @Column(columnDefinition = "TEXT")
    private String dislikedFoods;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecommendedFood> recommendedFoods = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public RecommendationSession(Member member, String context,
                                  String dietStatus, String veganOption, String spiceLevel,
                                  String foodTypes, String tastes, String dislikedFoods) {
        this.member = member;
        this.context = context;
        this.dietStatus = dietStatus;
        this.veganOption = veganOption;
        this.spiceLevel = spiceLevel;
        this.foodTypes = foodTypes;
        this.tastes = tastes;
        this.dislikedFoods = dislikedFoods;
    }

    public void addRecommendedFood(RecommendedFood food) {
        this.recommendedFoods.add(food);
        food.setSession(this);
    }
}
