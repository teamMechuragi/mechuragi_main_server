package com.mechuragi.mechuragi_server.domain.preference.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "preference_food_types")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PreferenceFoodType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preference_id", nullable = false)
    private FoodPreference preference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FoodType foodType;

    public enum FoodType {
        한식, 중식, 일식, 양식, 아시안, 디저트, 기타
    }

    @Builder
    public PreferenceFoodType(FoodPreference preference, FoodType foodType) {
        this.preference = preference;
        this.foodType = foodType;
    }
}