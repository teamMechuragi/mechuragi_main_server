package com.mechuragi.mechuragi_server.domain.recommend.entity;

import com.mechuragi.mechuragi_server.domain.member.entity.Member;
import com.mechuragi.mechuragi_server.domain.preference.entity.FoodPreference;
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

    // 추천 요청 컨텍스트 (날씨, 시간 등 상황 정보)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "session_context", joinColumns = @JoinColumn(name = "session_id"))
    @Column(name = "context_item")
    private List<String> context = new ArrayList<>();

    // 추천 시점의 취향 스냅샷 (Preference Snapshot)
    private Integer numberOfDiners;

    @Enumerated(EnumType.STRING)
    @Column(name = "diet_status")
    private FoodPreference.DietStatus dietStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "vegan_option")
    private FoodPreference.VeganOption veganOption;

    @Enumerated(EnumType.STRING)
    @Column(name = "spice_level")
    private FoodPreference.SpiceLevel spiceLevel;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "session_preferred_food_types", joinColumns = @JoinColumn(name = "session_id"))
    @Column(name = "food_type")
    private List<String> preferredFoodTypes = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "session_preferred_tastes", joinColumns = @JoinColumn(name = "session_id"))
    @Column(name = "taste")
    private List<String> preferredTastes = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "session_avoided_foods", joinColumns = @JoinColumn(name = "session_id"))
    @Column(name = "food_name")
    private List<String> avoidedFoods = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "session_allergies", joinColumns = @JoinColumn(name = "session_id"))
    @Column(name = "allergy_name")
    private List<String> allergies = new ArrayList<>();

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecommendedFood> recommendedFoods = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public RecommendationSession(Member member, List<String> context,
                                  Integer numberOfDiners,
                                  FoodPreference.DietStatus dietStatus,
                                  FoodPreference.VeganOption veganOption,
                                  FoodPreference.SpiceLevel spiceLevel,
                                  List<String> preferredFoodTypes,
                                  List<String> preferredTastes,
                                  List<String> avoidedFoods,
                                  List<String> allergies) {
        this.member = member;
        if (context != null) this.context = new ArrayList<>(context);
        this.numberOfDiners = numberOfDiners;
        this.dietStatus = dietStatus;
        this.veganOption = veganOption;
        this.spiceLevel = spiceLevel;
        if (preferredFoodTypes != null) this.preferredFoodTypes = new ArrayList<>(preferredFoodTypes);
        if (preferredTastes != null) this.preferredTastes = new ArrayList<>(preferredTastes);
        if (avoidedFoods != null) this.avoidedFoods = new ArrayList<>(avoidedFoods);
        if (allergies != null) this.allergies = new ArrayList<>(allergies);
    }

    public void addRecommendedFood(RecommendedFood food) {
        this.recommendedFoods.add(food);
        food.setSession(this);
    }
}
