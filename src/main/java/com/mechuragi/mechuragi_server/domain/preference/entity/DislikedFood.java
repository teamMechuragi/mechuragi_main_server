package com.mechuragi.mechuragi_server.domain.preference.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "disliked_foods")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DislikedFood {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preference_id", nullable = false)
    private FoodPreference preference;

    @Column(nullable = false, length = 100)
    private String foodName;

    @Builder
    public DislikedFood(FoodPreference preference, String foodName) {
        this.preference = preference;
        this.foodName = foodName;
    }
}