package com.mechuragi.mechuragi_server.domain.preference.entity;

import com.mechuragi.mechuragi_server.domain.member.entity.Member;
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
@Table(name = "food_preferences")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class FoodPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 100)
    private String preferenceName;

    @Column(nullable = false)
    private Boolean isActive = false;

    @Column(nullable = false)
    private Integer numberOfDiners = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "diet_status", nullable = false)
    private DietStatus dietStatus = DietStatus.NONE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VeganOption veganOption = VeganOption.NONE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpiceLevel spiceLevel = SpiceLevel.MILD;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "preferred_food_types", joinColumns = @JoinColumn(name = "preference_id"))
    @Column(name = "food_type")
    private List<String> preferredFoodTypes = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "preferred_tastes", joinColumns = @JoinColumn(name = "preference_id"))
    @Column(name = "taste")
    private List<String> preferredTastes = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "avoided_foods", joinColumns = @JoinColumn(name = "preference_id"))
    @Column(name = "food_name")
    private List<String> avoidedFoods = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "allergies", joinColumns = @JoinColumn(name = "preference_id"))
    @Column(name = "allergy_name")
    private List<String> allergies = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public enum DietStatus {
        NONE("해당 없음"),
        WEIGHT_LOSS("다이어트 중"),
        BULKING("근성장"),
        MAINTENANCE("유지어터");

        private final String description;
        DietStatus(String description) { this.description = description; }
        public String getDescription() { return description; }
    }

    public enum VeganOption {
        NONE("해당 없음 (일반식)"),
        VEGAN("비건 (완전 채식)"),
        VEGETARIAN("베지테리언 (유제품/달걀 허용)"),
        PESCATARIAN("페스코 (생선까지 허용)"),
        FLEXITARIAN("플렉시테리언 (간헐적 채식)");

        private final String description;
        VeganOption(String description) { this.description = description; }
        public String getDescription() { return description; }
    }

    public enum SpiceLevel {
        VERY_MILD("맵찔이"),
        MILD("순한맛"),
        MEDIUM("신라면"),
        HOT("불닭"),
        EXTREME("핵불닭");

        private final String description;
        SpiceLevel(String description) { this.description = description; }
        public String getDescription() { return description; }
    }

    @Builder
    public FoodPreference(Member member, String preferenceName, Boolean isActive,
                          Integer numberOfDiners, DietStatus dietStatus, VeganOption veganOption,
                          SpiceLevel spiceLevel, List<String> preferredFoodTypes,
                          List<String> preferredTastes, List<String> avoidedFoods,
                          List<String> allergies) {
        this.member = member;
        this.preferenceName = preferenceName;
        this.isActive = isActive;
        this.numberOfDiners = numberOfDiners;
        this.dietStatus = dietStatus;
        this.veganOption = veganOption;
        this.spiceLevel = spiceLevel;
        if (preferredFoodTypes != null) this.preferredFoodTypes = new ArrayList<>(preferredFoodTypes);
        if (preferredTastes != null) this.preferredTastes = new ArrayList<>(preferredTastes);
        if (avoidedFoods != null) this.avoidedFoods = new ArrayList<>(avoidedFoods);
        if (allergies != null) this.allergies = new ArrayList<>(allergies);
    }

    public void updatePreference(String preferenceName, Integer numberOfDiners,
                                 DietStatus dietStatus, VeganOption veganOption, SpiceLevel spiceLevel,
                                 List<String> preferredFoodTypes, List<String> preferredTastes,
                                 List<String> avoidedFoods, List<String> allergies) {
        if (preferenceName != null) this.preferenceName = preferenceName;
        if (numberOfDiners != null) this.numberOfDiners = numberOfDiners;
        if (dietStatus != null) this.dietStatus = dietStatus;
        if (veganOption != null) this.veganOption = veganOption;
        if (spiceLevel != null) this.spiceLevel = spiceLevel;
        if (preferredFoodTypes != null) {
            this.preferredFoodTypes.clear();
            this.preferredFoodTypes.addAll(preferredFoodTypes);
        }
        if (preferredTastes != null) {
            this.preferredTastes.clear();
            this.preferredTastes.addAll(preferredTastes);
        }
        if (avoidedFoods != null) {
            this.avoidedFoods.clear();
            this.avoidedFoods.addAll(avoidedFoods);
        }
        if (allergies != null) {
            this.allergies.clear();
            this.allergies.addAll(allergies);
        }
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }
}
