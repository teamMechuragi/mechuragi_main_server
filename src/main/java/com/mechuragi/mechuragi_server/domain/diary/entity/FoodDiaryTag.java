package com.mechuragi.mechuragi_server.domain.diary.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "food_diary_tags",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_diary_tag", columnNames = {"food_diary_id", "tag_id"})
        },
        indexes = {
                @Index(name = "idx_diary_id", columnList = "food_diary_id"),
                @Index(name = "idx_tag_id", columnList = "tag_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class FoodDiaryTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_diary_id", nullable = false)
    private FoodDiary foodDiary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public FoodDiaryTag(FoodDiary foodDiary, Tag tag) {
        this.foodDiary = foodDiary;
        this.tag = tag;
    }

    // 양방향 관계 설정용 (package-private으로 제한)
    void setFoodDiary(FoodDiary foodDiary) {
        this.foodDiary = foodDiary;
    }
}
