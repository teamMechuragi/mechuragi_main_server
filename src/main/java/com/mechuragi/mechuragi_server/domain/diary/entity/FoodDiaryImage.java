package com.mechuragi.mechuragi_server.domain.diary.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "food_diary_images", indexes = {
        @Index(name = "idx_diary_id", columnList = "food_diary_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FoodDiaryImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_diary_id", nullable = false)
    private FoodDiary foodDiary;

    @Column(nullable = false, length = 500)
    private String imageUrl;

    @Column(nullable = false)
    private Integer displayOrder;

    @Builder
    public FoodDiaryImage(FoodDiary foodDiary, String imageUrl, Integer displayOrder) {
        this.foodDiary = foodDiary;
        this.imageUrl = imageUrl;
        this.displayOrder = displayOrder;
    }

    // 양방향 관계 설정용 (package-private으로 제한)
    void setFoodDiary(FoodDiary foodDiary) {
        this.foodDiary = foodDiary;
    }
}
