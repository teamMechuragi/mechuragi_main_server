package com.mechuragi.mechuragi_server.domain.diary.entity;

import com.mechuragi.mechuragi_server.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "food_diaries",
        indexes = {
                @Index(name = "idx_member_diary_date", columnList = "member_id, diary_date")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_member_diary_date", columnNames = {"member_id", "diary_date"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class FoodDiary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, precision = 2, scale = 1)
    private BigDecimal rating;

    @Column(nullable = false, name = "diary_date")
    private LocalDate diaryDate;

    @OneToMany(mappedBy = "foodDiary", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FoodDiaryImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "foodDiary", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FoodDiaryTag> diaryTags = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public FoodDiary(Member member, String title, String content, BigDecimal rating, LocalDate diaryDate) {
        this.member = member;
        this.title = title;
        this.content = content;
        this.rating = rating;
        this.diaryDate = diaryDate;
    }

    // 일기 수정 (날짜는 변경 불가)
    public void update(String title, String content, BigDecimal rating) {
        this.title = title;
        this.content = content;
        this.rating = rating;
    }

    // 이미지 추가
    public void addImage(FoodDiaryImage image) {
        this.images.add(image);
        image.setFoodDiary(this);
    }

    // 모든 이미지 제거
    public void clearImages() {
        this.images.clear();
    }

    // 태그 추가
    public void addTag(FoodDiaryTag diaryTag) {
        this.diaryTags.add(diaryTag);
        diaryTag.setFoodDiary(this);
    }

    // 모든 태그 제거
    public void clearTags() {
        this.diaryTags.clear();
    }
}
