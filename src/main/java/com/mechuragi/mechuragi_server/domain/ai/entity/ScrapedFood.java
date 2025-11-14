package com.mechuragi.mechuragi_server.domain.ai.entity;

import com.mechuragi.mechuragi_server.domain.ai.entity.type.RecommendationType;
import com.mechuragi.mechuragi_server.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "Scraped_foods")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ScrapedFood {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecommendationType recommendationType;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String ingredients;

    @Column(length = 50)
    private String cookingTime;

    @Column(length = 50)
    private String difficulty;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public ScrapedFood(Member member, RecommendationType recommendationType,
                       String name, String description, String reason,
                       String ingredients, String cookingTime, String difficulty) {
        this.member = member;
        this.recommendationType = recommendationType;
        this.name = name;
        this.description = description;
        this.reason = reason;
        this.ingredients = ingredients;
        this.cookingTime = cookingTime;
        this.difficulty = difficulty;
    }
}
