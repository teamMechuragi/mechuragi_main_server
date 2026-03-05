# 음식 취향(Preference) 도메인 리팩토링

> PR #89 `fix/preference` → `dev`
> 대상 파일: `domain/preference/entity/`, `domain/preference/service/`, `domain/preference/repository/`

---

## 개요

기존 버전에서 선호 음식 유형, 선호 맛, 기피 음식을 각각 별도 엔티티와 Repository로 관리하던 구조를 `@ElementCollection` 기반으로 통합 리팩토링.

**핵심 문제**
- `PreferenceFoodType`, `PreferenceTaste`, `DislikedFood` 3개의 별도 엔티티 → 3개의 Repository → 저장/수정/삭제마다 각각 수동 처리 필요
- 수정 시 `deleteByPreferenceId` 후 `saveAll` 재저장 패턴 반복 → 불필요한 쿼리 증가
- Enum 상수가 한글로 선언되어 JPA 직렬화 불안정 및 가독성 저하
- `allergyInfo`가 단일 TEXT 컬럼 → 구조화 불가, AI 추천 API 연동 시 파싱 필요

---

## 1. 엔티티 구조 변경

### 1-1. 별도 엔티티 3개 삭제 → @ElementCollection 통합

**Before**

```
entity/
├── FoodPreference.java
├── PreferenceFoodType.java   ← @Entity, @ManyToOne
├── PreferenceTaste.java      ← @Entity, @ManyToOne
└── DislikedFood.java         ← @Entity, @ManyToOne

repository/
├── FoodPreferenceRepository.java
├── PreferenceFoodTypeRepository.java
├── PreferenceTasteRepository.java
└── DislikedFoodRepository.java
```

**After**

```
entity/
└── FoodPreference.java   ← @ElementCollection으로 모든 컬렉션 통합

repository/
└── FoodPreferenceRepository.java
```

자식 엔티티 3개, Repository 3개 제거. `FoodPreference`에 `@ElementCollection`으로 컬렉션 테이블 직접 매핑.

```java
// After: FoodPreference.java
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
```

---

### 1-2. 필드 구조 변경

**Before**

| 필드명 | 타입 | 문제 |
|--------|------|------|
| `allergyInfo` | `String` (TEXT 컬럼) | 단일 문자열로 관리 → AI API 연동 시 파싱 필요 |
| `isOnDiet` | `DietStatus` | 네이밍이 boolean 느낌, 실제로는 enum |
| `dislikedFoods` | DTO 내 `List<String>` | 엔티티에는 `DislikedFood` 자식 엔티티로 따로 저장 |

**After**

| 필드명 | 타입 | 개선 |
|--------|------|------|
| `allergies` | `List<String>` (@ElementCollection) | 구조화된 리스트로 AI API에 직접 전달 가능 |
| `dietStatus` | `DietStatus` | 의미 명확, 네이밍 통일 |
| `avoidedFoods` | `List<String>` (@ElementCollection) | 엔티티에 직접 포함, 별도 Repository 불필요 |

---

### 1-3. Enum 전면 개선

**Before**

```java
public enum DietStatus {
    다이어트_중, 해당_없음   // 한글 상수, 2가지 선택지
}

public enum VeganOption {
    락토_베지테리언, 락토_오보_베지테리언, 비건, 오보_베지테리언,
    페스코_베지테리언, 폴로_베지테리언, 프루테리언, 플렉시테리언, 해당없음  // 9가지
}

public enum SpiceLevel {
    맵찔이, 순한맛, 신라면, 불닭, 핵불닭   // 한글 상수
}
```

**After**

```java
public enum DietStatus {
    NONE("해당 없음"),
    WEIGHT_LOSS("다이어트 중"),
    BULKING("근성장"),
    MAINTENANCE("유지어터");   // 4가지로 확장, description 분리

    private final String description;
    DietStatus(String description) { this.description = description; }
}

public enum VeganOption {
    NONE("해당 없음 (일반식)"),
    VEGAN("비건 (완전 채식)"),
    VEGETARIAN("베지테리언 (유제품/달걀 허용)"),
    PESCATARIAN("페스코 (생선까지 허용)"),
    FLEXITARIAN("플렉시테리언 (간헐적 채식)");   // 9개 → 5개로 간소화
}

public enum SpiceLevel {
    VERY_MILD("맵찔이"), MILD("순한맛"), MEDIUM("신라면"), HOT("불닭"), EXTREME("핵불닭");
}
```

한글 상수명은 JPA `@Enumerated(EnumType.STRING)` 저장 시 인코딩 이슈 가능성 존재. 영문 상수명 + `description` 필드 분리로 DB 저장값과 표시 텍스트를 분리.

---

## 2. 서비스 로직 변경

### 2-1. 저장 로직 단순화

**Before**

```java
FoodPreference savedPreference = foodPreferenceRepository.save(preference);

// 자식 엔티티 각각 별도 저장
saveFoodTypes(savedPreference, request.getPreferredFoodTypes());
saveTastes(savedPreference, request.getPreferredTastes());
saveDislikedFoods(savedPreference, request.getDislikedFoods());
```

**After**

```java
// @ElementCollection이 부모와 함께 저장됨
return foodPreferenceRepository.save(preference).getId();
```

`saveFoodTypes`, `saveTastes`, `saveDislikedFoods` 헬퍼 메서드 전부 제거.

---

### 2-2. 수정 로직 단순화

**Before**

```java
// 수정 시: 기존 자식 데이터 전부 삭제 후 재저장
if (request.getPreferredFoodTypes() != null) {
    preferenceFoodTypeRepository.deleteByPreferenceId(preferenceId);
    saveFoodTypes(preference, request.getPreferredFoodTypes());
}
if (request.getPreferredTastes() != null) {
    preferenceTasteRepository.deleteByPreferenceId(preferenceId);
    saveTastes(preference, request.getPreferredTastes());
}
if (request.getDislikedFoods() != null) {
    dislikedFoodRepository.deleteByPreferenceId(preferenceId);
    saveDislikedFoods(preference, request.getDislikedFoods());
}
```

**After**

```java
// 엔티티 메서드 내에서 clear() + addAll() 처리
preference.updatePreference(
    request.getPreferenceName(), request.getNumberOfDiners(),
    request.getDietStatus(), request.getVeganOption(), request.getSpiceLevel(),
    request.getPreferredFoodTypes(), request.getPreferredTastes(),
    request.getAvoidedFoods(), request.getAllergies()
);
```

---

### 2-3. 삭제 로직 단순화

**Before**

```java
// 자식 테이블 3개 수동 삭제 후 부모 삭제
preferenceFoodTypeRepository.deleteByPreferenceId(preferenceId);
preferenceTasteRepository.deleteByPreferenceId(preferenceId);
dislikedFoodRepository.deleteByPreferenceId(preferenceId);
foodPreferenceRepository.deleteByIdAndMemberId(preferenceId, memberId);
```

**After**

```java
// @ElementCollection은 부모 삭제 시 컬렉션 테이블 데이터도 자동 삭제
foodPreferenceRepository.deleteByIdAndMemberId(preferenceId, memberId);
```

---

### 2-4. 조회 로직 단순화

**Before**

```java
// 조회 시: 3개 Repository에서 각각 조회 후 DTO 조립
List<String> foodTypes = preferenceFoodTypeRepository.findByPreferenceId(preferenceId)
        .stream().map(type -> type.getFoodType().name()).collect(Collectors.toList());

List<String> tastes = preferenceTasteRepository.findByPreferenceId(preferenceId)
        .stream().map(taste -> taste.getTasteType().name()).collect(Collectors.toList());

List<String> dislikedFoods = dislikedFoodRepository.findByPreferenceId(preferenceId)
        .stream().map(DislikedFood::getFoodName).collect(Collectors.toList());
```

**After**

```java
// 엔티티에서 직접 접근
.preferredFoodTypes(preference.getPreferredFoodTypes())
.preferredTastes(preference.getPreferredTastes())
.avoidedFoods(preference.getAvoidedFoods())
.allergies(preference.getAllergies())
```

---

## 3. 연관 도메인 변경 (recommend)

`RecommendedFood.description` 필드 삭제. `reason` 필드와 내용이 중복되어 AI 서버 응답 DTO, 엔티티, 매퍼에서 함께 제거.

| 파일 | 변경 내용 |
|------|-----------|
| `RecommendedFood.java` | `description` 필드 삭제 |
| `RecommendedFoodResponse.java` | `description` 필드 삭제 |
| `SaveRecommendationRequest.java` | `description` 필드 삭제 |
| `RecommendedFoodMapper.java` | `description` 매핑 제거 |

---

## 4. 변경 요약

| 항목 | Before | After |
|------|--------|-------|
| 엔티티 수 | 4개 (FoodPreference + 자식 3개) | 1개 |
| Repository 수 | 4개 | 1개 |
| 컬렉션 저장 방식 | 자식 엔티티 `saveAll` | `@ElementCollection` 자동 처리 |
| 컬렉션 수정 방식 | `deleteByPreferenceId` + `saveAll` 반복 | 엔티티 메서드 내 `clear()` + `addAll()` |
| 컬렉션 삭제 방식 | 자식 테이블 3개 수동 삭제 | 부모 삭제 시 자동 삭제 |
| Enum 상수명 | 한글 | 영문 상수 + `description` 필드 분리 |
| `DietStatus` 선택지 | 2개 | 4개 (NONE / WEIGHT_LOSS / BULKING / MAINTENANCE) |
| `VeganOption` 선택지 | 9개 | 5개 |
| `allergyInfo` | `String` TEXT 컬럼 | `List<String>` @ElementCollection |
| 총 변경 | +135줄 / -308줄 | 코드량 약 55% 감소 |
