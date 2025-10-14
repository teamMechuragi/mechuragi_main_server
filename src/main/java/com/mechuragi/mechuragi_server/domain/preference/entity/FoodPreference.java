package com.mechuragi.mechuragi_server.domain.preference.entity;

import com.mechuragi.mechuragi_server.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

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
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String preferenceName;

    @Column(nullable = false)
    private Boolean isActive = false;

    @Column(nullable = false)
    private Integer numberOfDiners = 1;

    @Column(columnDefinition = "TEXT")
    private String allergyInfo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DietStatus isOnDiet = DietStatus.해당_없음;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VeganOption veganOption = VeganOption.해당없음;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpiceLevel spiceLevel = SpiceLevel.순한맛;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public enum DietStatus {
        다이어트_중, 해당_없음
    }

    public enum VeganOption {
        락토_베지테리언, 락토_오보_베지테리언, 비건, 오보_베지테리언,
        페스코_베지테리언, 폴로_베지테리언, 프루테리언, 플렉시테리언, 해당없음
    }

    public enum SpiceLevel {
        맵찔이, 순한맛, 신라면, 불닭, 핵불닭
    }

    @Builder
    public FoodPreference(User user, String preferenceName, Boolean isActive,
                         Integer numberOfDiners, String allergyInfo,
                         DietStatus isOnDiet, VeganOption veganOption, SpiceLevel spiceLevel) {
        this.user = user;
        this.preferenceName = preferenceName;
        this.isActive = isActive;
        this.numberOfDiners = numberOfDiners;
        this.allergyInfo = allergyInfo;
        this.isOnDiet = isOnDiet;
        this.veganOption = veganOption;
        this.spiceLevel = spiceLevel;
    }

    public void updatePreference(String preferenceName, Integer numberOfDiners,
                               String allergyInfo, DietStatus isOnDiet,
                               VeganOption veganOption, SpiceLevel spiceLevel) {
        if (preferenceName != null) this.preferenceName = preferenceName;
        if (numberOfDiners != null) this.numberOfDiners = numberOfDiners;
        if (allergyInfo != null) this.allergyInfo = allergyInfo;
        if (isOnDiet != null) this.isOnDiet = isOnDiet;
        if (veganOption != null) this.veganOption = veganOption;
        if (spiceLevel != null) this.spiceLevel = spiceLevel;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }
}