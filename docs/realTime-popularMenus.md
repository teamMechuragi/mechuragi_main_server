# 실시간 인기 메뉴 조회 기능 설계

## 📋 개요

Hot 투표(인기 투표)의 메뉴 옵션들을 집계하여 실시간으로 가장 인기 있는 메뉴 Top 10을 제공하는 기능입니다.

---

## 🔄 처리 흐름

### Step 1. Hot 투표 데이터 가져오기 (실시간 버전)

```java
// ⚠️ 주의: getHotVotes()는 5분간 캐싱되므로 직접 조회 필요!
Set<String> topVoteIds = redisTemplate.opsForZSet()
        .reverseRange("vote:hot", 0, size - 1);

List<Long> voteIds = topVoteIds.stream()
        .map(Long::valueOf)
        .collect(Collectors.toList());

List<VotePost> hotVotes = votePostRepository.findAllById(voteIds);

// Redis 순서대로 정렬
Map<Long, VotePost> voteMap = hotVotes.stream()
        .collect(Collectors.toMap(VotePost::getId, v -> v));

List<VoteResponseDTO> hotVoteDTOs = voteIds.stream()
        .map(voteMap::get)
        .filter(Objects::nonNull)
        .map(v -> VoteResponseDTO.from(v, redisTemplate))
        .collect(Collectors.toList());
```

**왜 직접 조회하나요?**
- `VotePostService.getHotVotes()`는 `@Cacheable("hotVotes")`로 **5분간 캐싱**됨
- `vote:hot` Sorted Set은 실시간 업데이트되지만, 메서드 결과는 5분 동안 고정
- **실시간 인기 메뉴**를 위해서는 캐시를 우회하고 직접 조회해야 함

**대안: 별도 메서드 생성**
```java
// VotePostService에 추가
public List<VoteResponseDTO> getHotVotesRealtime(int size) {
    // 위의 직접 조회 로직
    // @Cacheable 없이 항상 최신 데이터 반환
}
```

### Step 2. 모든 메뉴 옵션 추출 및 실시간 투표율 계산

```java
List<MenuOptionData> allOptions = new ArrayList<>();

for (VoteResponseDTO vote : hotVoteDTOs) {
    // 실시간 총 참여자 수 조회 (Redis)
    String participantsKey = "vote:" + vote.getId() + ":participants";
    int totalParticipants = Optional.ofNullable(
        redisTemplate.opsForValue().get(participantsKey)
    ).map(Integer::parseInt).orElse(0);

    for (VoteOptionResponseDTO option : vote.getOptions()) {
        // ⚠️ 주의: DTO의 votePercentage는 DB 기반이므로 재계산 필요!
        double realtimeVotePercentage = totalParticipants > 0
            ? (double) option.getVoteCount() / totalParticipants * 100.0
            : 0.0;

        allOptions.add(new MenuOptionData(
            option.getOptionText(),
            option.getVoteCount(),
            realtimeVotePercentage,  // 실시간 재계산된 투표율
            vote.getCreatedAt()
        ));
    }
}
```

- 각 투표(`VoteResponseDTO`)의 `options` 리스트에서 `VoteOptionResponseDTO` 전부 수집
- **중복 포함**: 같은 메뉴가 여러 투표에 등장하면 각 건마다 집계

**⚠️ 실시간성 확보 포인트:**

| 필드 | 데이터 소스 | 실시간 여부 |
|------|------------|------------|
| `optionText` | DB | ✅ (변경 안 됨) |
| `voteCount` | Redis (`vote:{id}:option:{optionId}:count`) | ✅ 실시간 |
| `votePercentage` (DTO) | Entity의 participations.size() | ❌ DB 기반 |
| `votePercentage` (재계산) | Redis 기반 계산 | ✅ 실시간 |

**왜 재계산이 필요한가?**
- `VoteOption.getVotePercentage()`는 Entity의 `participations` 컬렉션 사용 (DB)
- `voteCount`는 Redis에서 실시간 조회하지만, 분모(`totalParticipants`)는 DB 기반
- 실시간성을 위해 **분자(voteCount)와 분모(totalParticipants) 모두 Redis에서 조회**

### Step 3. 메뉴 문자열 정규화 및 동의어 처리

#### 3.1 정규화 (Normalization)

```java
String normalized = normalizeMenuName(optionText);
```

**처리 내용:**
- 공백 제거 또는 통일 (trim, 연속 공백 → 단일 공백)
- 특수문자 제거 (`!`, `?`, `~`, `*` 등)
- 이모지 제거
- 대소문자 통일 (소문자 변환)

**예시:**
```
"🍜 마라탕!!!" → "마라탕"
"떡 볶 이" → "떡볶이"
"Pasta" → "pasta"
```

#### 3.2 동의어 처리 (Synonym Mapping)

```java
String canonical = applySynonym(normalized);
```

**동의어 테이블 예시:**
```java
Map<String, String> synonyms = Map.of(
    "파스타", "스파게티",
    "짜장면", "자장면",
    "라면", "라멘",
    "돈까스", "돈카츠"
);
```

**관리 전략:**
- **초기**: `Map` 또는 `Enum`으로 하드코딩
- **중기**: `application.yml` 또는 JSON 파일로 외부화
- **장기**: Admin API로 동적 관리 가능하도록 확장

**결과**: 동일한 메뉴는 하나의 키로 통합

### Step 4. 메뉴별 점수 집계 (개선)

#### 점수 구성 요소

| 요소 | 계산 방식 | 비중 | 설명 |
|------|----------|------|------|
| **언급 횟수** | `mentionCount × 10.0` | 높음 | 여러 투표에 등장할수록 인기 |
| **평균 투표율** | `avgVotePercentage × 0.01` | 중간 | 각 투표에서 얼마나 선택받았는지 |
| **최근성** | `recencyScore × 2.0` | 중간 | 최근 생성된 투표일수록 가중치 ⭐ |
| **기본 점수** | `+1.0` | 낮음 | 모든 메뉴에 기본 점수 부여 |

#### 최종 점수 공식 (개선)

```java
double menuScoreDTO = (mentionCount * 10.0)
                 + (avgVotePercentage * 0.01)
                 + (recencyScore * 2.0)  // 신규 추가
                 + 1.0;
```

#### 최근성 점수 계산 (선택 사항)

```java
// 투표 생성일 기준 최근성 계산
long daysSinceCreated = ChronoUnit.DAYS.between(vote.getCreatedAt(), LocalDateTime.now());
double recencyScore = Math.max(0, 1.0 - (daysSinceCreated / 7.0));  // 7일 기준
```

- 오늘 생성된 투표: `recencyScore = 1.0`
- 7일 전 생성된 투표: `recencyScore = 0.0`
- **효과**: 최근 트렌드를 더 잘 반영

#### 계산 예시

**예제 1: 마라탕**
```
투표1: votePercentage = 40%, createdAt = 1일 전
투표2: votePercentage = 35%, createdAt = 3일 전

mentionCount = 2
avgVotePercentage = (40 + 35) / 2 = 37.5
avgRecency = ((1 - 1/7) + (1 - 3/7)) / 2 ≈ 0.714

최종 score = (2 * 10.0) + (37.5 * 0.5) + (0.714 * 2.0) + 1.0
           = 20 + 18.75 + 1.428 + 1.0
           = 41.178
```

**예제 2: 스테이크**
```
투표1: votePercentage = 31.4%, createdAt = 2일 전

mentionCount = 1
avgVotePercentage = 31.4
avgRecency = 1 - 2/7 ≈ 0.714

최종 score = (1 * 10.0) + (31.4 * 0.5) + (0.714 * 2.0) + 1.0
           = 10 + 15.7 + 1.428 + 1.0
           = 28.128
```

### Step 5. 메뉴별 그룹화 및 병합

```java
Map<String, MenuScore> menuMap = new HashMap<>();

for (MenuOptionData option : allOptions) {
    // 정규화 및 동의어 처리
    String canonical = normalizeAndApplySynonym(option.getOptionText());

    // 최근성 계산
    long daysSinceCreated = ChronoUnit.DAYS.between(
        option.getCreatedAt(),
        LocalDateTime.now()
    );
    double recencyScore = Math.max(0, 1.0 - (daysSinceCreated / 7.0));

    // 메뉴별 데이터 누적
    menuMap.computeIfAbsent(canonical, k -> new MenuScore(canonical));
    menuMap.get(canonical).addData(
        option.getRealtimeVotePercentage(),  // 실시간 재계산된 투표율 사용
        recencyScore
    );
}
```

- 동일한 메뉴명(정규화 후)끼리 그룹화
- 언급 횟수, 평균 투표율, 평균 최근성 누적

**내부 구조 (`MenuScore` 클래스):**
```java
public class MenuScore {
    private String menuName;
    private int mentionCount = 0;
    private List<Double> votePercentages = new ArrayList<>();
    private List<Double> recencyScores = new ArrayList<>();

    public void addData(double votePercentage, double recency) {
        mentionCount++;
        votePercentages.add(votePercentage);
        recencyScores.add(recency);
    }

    public double getScore() {
        double avgVotePercentage = votePercentages.stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);

        double avgRecency = recencyScores.stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);

        return (mentionCount * 10.0)
             + (avgVotePercentage * 0.01)
             + (avgRecency * 2.0)
             + 1.0;
    }
}
```

### Step 6. 상위 10개 메뉴 정렬 및 반환

```java
List<PopularMenuResponseDTO> topMenus = menuMap.values().stream()
    .sorted(Comparator.comparingDouble(MenuScore::getScore).reversed())
    .limit(10)
    .map(this::toDTO)
    .collect(Collectors.toList());
```

- `menuScoreDTO` 기준 내림차순 정렬
- 상위 10개 메뉴만 추출
- 중복 제거 완료

---

## 📤 응답 형식

### DTO 구조

**내부 데이터 클래스 (`MenuOptionData`):**
```java
@Getter
@AllArgsConstructor
public class MenuOptionData {
    private String optionText;              // 메뉴명
    private int voteCount;                  // 투표 수 (Redis)
    private double realtimeVotePercentage;  // 실시간 투표율 (Redis 기반 재계산)
    private LocalDateTime createdAt;        // 투표 생성일 (최근성 계산용)
}
```

**응답 DTO (`PopularMenuResponseDTO`):**
```java
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PopularMenuResponseDTO {
    private String menu;                    // 메뉴명
    private double score;                   // 최종 점수
    private int mentionCount;               // 언급 횟수
    private double averageVotePercentage;   // 평균 투표율 (실시간)
    private double averageRecency;          // 평균 최근성
}
```

### 응답 예시

```json
[
  {
    "menu": "마라탕",
    "score": 41.178,
    "mentionCount": 2,
    "averageVotePercentage": 37.5,
    "averageRecency": 0.714
  },
  {
    "menu": "스테이크",
    "score": 28.128,
    "mentionCount": 1,
    "averageVotePercentage": 31.4,
    "averageRecency": 0.714
  },
  {
    "menu": "김치찌개",
    "score": 25.5,
    "mentionCount": 3,
    "averageVotePercentage": 22.0,
    "averageRecency": 0.5
  }
]
```

---

## 🗄️ 캐싱 및 갱신 전략

### ⚠️ 중요: 실시간성 vs 성능 트레이드오프

**실시간 데이터 소스:**
- `vote:hot` Sorted Set: 투표 참여/좋아요 시 즉시 업데이트
- `vote:{id}:participants`: 투표 참여/취소 시 즉시 업데이트
- `vote:{id}:option:{optionId}:count`: 투표 시 즉시 업데이트

**캐싱이 필요한 이유:**
- Hot 투표 50~100개 조회 + 각 옵션별 계산 → DB/Redis 부하
- 요청마다 재계산 시 응답 시간 증가
- 적절한 캐싱으로 실시간성과 성능 균형 유지

### 캐싱 전략 옵션

#### 옵션 1: 짧은 TTL 캐싱 (권장) ⭐

```java
@Cacheable(value = "popularMenus", key = "'realtime'")
public List<PopularMenuResponseDTO> getPopularMenus() {
    return calculatePopularMenusRealtime();
}

// RedisCacheConfig.java에 커스텀 설정 추가
@Bean
public RedisCacheConfiguration popularMenusCacheConfig() {
    return RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(1));  // 1분 TTL
}
```

**특징:**
- 최대 1분 지연 (Hot 투표가 5분 캐싱보다 훨씬 실시간)
- 중간 수준의 DB/Redis 부하
- **추천 이유**: 실시간성과 성능의 균형

#### 옵션 2: 캐싱 없이 매번 계산 (완전 실시간)

```java
// @Cacheable 없음
public List<PopularMenuResponseDTO> getPopularMenus() {
    return calculatePopularMenusRealtime();
}
```

**특징:**
- 완벽한 실시간성
- 높은 DB/Redis 부하
- 사용자가 적거나, 실시간성이 최우선일 때 선택

#### 옵션 3: 스케줄러 + 수동 캐시 관리

```java
@Scheduled(fixedRate = 60000)  // 1분마다
public void updatePopularMenusCache() {
    List<PopularMenuResponseDTO> menus = calculatePopularMenusRealtime();
    String json = objectMapper.writeValueAsString(menus);
    redisTemplate.opsForValue().set(
        "popularMenus:realtime",
        json,
        Duration.ofMinutes(2)  // 2분 TTL (스케줄러 실패 대비)
    );
}

public List<PopularMenuResponseDTO> getPopularMenus() {
    String cached = redisTemplate.opsForValue().get("popularMenus:realtime");
    if (cached != null) {
        return objectMapper.readValue(cached,
            new TypeReference<List<PopularMenuResponseDTO>>() {});
    }
    return calculatePopularMenusRealtime();  // fallback
}
```

**특징:**
- 백그라운드 갱신으로 사용자 요청 즉시 응답
- 1분마다 자동 갱신
- 복잡도 증가

### 권장 설정

**개발/테스트 환경:**
```java
// 캐싱 없음 또는 10초 TTL
.entryTtl(Duration.ofSeconds(10));
```

**프로덕션 환경:**
```java
// 1분 TTL (옵션 1)
.entryTtl(Duration.ofMinutes(1));
```

---

## 🏗️ 아키텍처 권장사항

### 서비스 분리

```
📦 domain.menu (신규 도메인)
 ├── 📂 controller
 │   └── PopularMenuController.java
 ├── 📂 service
 │   └── PopularMenuService.java        // 인기 메뉴 집계 로직
 ├── 📂 dto
 │   └── PopularMenuResponseDTO.java
 └── 📂 util
     └── MenuNormalizer.java             // 정규화 유틸
```

**이유:**
- 단일 책임 원칙(SRP) 준수
- `VotePostService`의 책임 과중 방지
- 향후 메뉴 관련 기능 확장 용이

### 의존성 다이어그램

```
PopularMenuService
    ↓ (의존)
VotePostService.getHotVotes()
```

---

## ⚙️ 구현 체크리스트

### 핵심 기능
- [ ] `PopularMenuService` 생성
- [ ] **`getHotVotesRealtime()` 메서드 구현** (vote:hot 직접 조회, 캐시 우회)
- [ ] **`MenuOptionData` 내부 클래스 생성** (실시간 투표율 포함)
- [ ] **실시간 투표율 재계산 로직** (Redis 기반 totalParticipants 조회)
- [ ] `MenuScore` 내부 클래스 생성 (점수 계산 및 집계)
- [ ] 점수 계산 로직 구현 (언급 횟수, 투표율, 최근성)

### 유틸 및 지원 기능
- [ ] 메뉴 정규화 유틸 구현 (`MenuNormalizer`)
  - [ ] 공백/특수문자/이모지 제거
  - [ ] 대소문자 통일
- [ ] 동의어 테이블 정의 (초기: 하드코딩)
  - [ ] `Map<String, String>` 또는 Enum 사용
  - [ ] 추후 외부화 가능하도록 설계

### 캐싱 및 성능
- [ ] Redis 캐싱 설정 커스터마이징
  - [ ] `popularMenus` 전용 캐시 설정 (1분 TTL)
  - [ ] 또는 캐싱 없이 실시간 조회 선택
- [ ] (선택) 스케줄러 설정 (`@Scheduled`)

### API 및 테스트
- [ ] `PopularMenuController` 생성
- [ ] API 엔드포인트 생성 (`GET /api/menus/popular`)
- [ ] 단위 테스트 작성
  - [ ] 정규화 로직 테스트
  - [ ] 동의어 처리 테스트
  - [ ] 점수 계산 테스트
- [ ] 통합 테스트 작성
  - [ ] 실시간 데이터 반영 검증
  - [ ] 캐싱 동작 검증

---

## 🔍 주의사항

### 1. ⚠️ 실시간성 확보 (매우 중요!)

**문제점:**
- `VotePostService.getHotVotes()`는 `@Cacheable`로 **5분간 캐싱**됨
- `VoteOption.getVotePercentage()`는 DB의 `participations` 컬렉션 기반

**해결 방법:**
- ✅ `vote:hot` Sorted Set을 **직접 조회**하여 캐시 우회
- ✅ `votePercentage`를 **Redis 기반으로 재계산**
  ```java
  // ❌ 잘못된 방법 (DB 기반)
  double percentage = option.getVotePercentage();

  // ✅ 올바른 방법 (Redis 기반)
  int totalParticipants = Integer.parseInt(
      redisTemplate.opsForValue().get("vote:" + voteId + ":participants")
  );
  double realtimePercentage = (double) voteCount / totalParticipants * 100.0;
  ```

### 2. 성능 최적화

**부하 예상:**
- Hot 투표 50개 × 평균 옵션 5개 = 250개 옵션 처리
- 각 투표마다 Redis 조회 1회 (`totalParticipants`)
- DB 조회: Hot 투표 목록 조회 1회

**최적화 방안:**
- 1분 TTL 캐싱으로 부하 분산 (권장)
- 또는 스케줄러로 백그라운드 갱신
- Redis Pipeline 사용 고려 (여러 키 한 번에 조회)

### 3. Redis 데이터 정합성

**확인 사항:**
- `RedisDataInitializer`에서 앱 시작 시 Redis 초기화 확인
- `vote:hot`, `vote:{id}:participants`, `vote:{id}:option:{optionId}:count` 키 존재 확인
- Redis 장애 시 fallback 로직 필요

**Fallback 예시:**
```java
int totalParticipants = Optional.ofNullable(
    redisTemplate.opsForValue().get(participantsKey)
)
.map(Integer::parseInt)
.orElseGet(() -> votePost.getTotalParticipants());  // DB fallback
```

### 4. 동의어 확장성

**단계별 접근:**
- **Phase 1**: 하드코딩 (Map 또는 Enum)
  ```java
  Map<String, String> synonyms = Map.of(
      "파스타", "스파게티",
      "짜장면", "자장면"
  );
  ```
- **Phase 2**: 외부 설정 (application.yml 또는 JSON 파일)
- **Phase 3**: Admin API로 동적 관리

### 5. 예외 처리

**처리 필요한 경우:**
- Hot 투표가 없는 경우 → 빈 리스트 반환
- Redis 키가 없는 경우 → DB fallback 또는 0으로 처리
- 정규화 후 메뉴명이 빈 문자열인 경우 → 필터링

### 6. 모니터링 포인트

**추적 필요한 지표:**
- 인기 메뉴 조회 응답 시간
- Redis 조회 횟수 및 시간
- 캐시 히트율 (`popularMenus`)
- Hot 투표 수 변동 추이

**로깅 권장:**
```java
log.info("인기 메뉴 계산 완료: Hot 투표 {}개, 총 옵션 {}개, 소요시간 {}ms",
    hotVotesCount, totalOptionsCount, duration);
```

---

## 📊 실시간성 보장 체크리스트

구현 시 다음 항목들을 반드시 확인하세요:

- [ ] `vote:hot` Sorted Set을 **직접 조회**하고 있는가?
- [ ] `getHotVotes()` 메서드를 사용하지 않는가? (5분 캐싱 문제)
- [ ] `totalParticipants`를 **Redis에서 조회**하고 있는가?
- [ ] `votePercentage`를 **재계산**하고 있는가?
- [ ] Redis fallback 로직이 구현되어 있는가?
- [ ] 적절한 캐싱 전략을 선택했는가? (1분 TTL 권장)
- [ ] 로깅 및 모니터링이 설정되어 있는가?

---

## 🎯 핵심 요약

### 실시간 인기 메뉴의 핵심 원칙

1. **실시간 데이터 소스 사용**
   - ✅ `vote:hot` 직접 조회 (캐시 우회)
   - ✅ Redis 기반 voteCount
   - ✅ Redis 기반 totalParticipants
   - ✅ Redis 기반 votePercentage 재계산

2. **적절한 캐싱 전략**
   - ⏱️ 1분 TTL 캐싱 (실시간성 + 성능 균형)
   - 또는 캐싱 없음 (완전 실시간, 높은 부하)

3. **견고한 예외 처리**
   - Redis 장애 시 DB fallback
   - 빈 데이터 처리
   - 정규화 예외 처리

### 주요 개선점 (기존 Hot 투표 대비)

| 항목 | Hot 투표 | 실시간 인기 메뉴 |
|------|---------|----------------|
| **Hot 목록 조회** | `@Cacheable` (5분) | `vote:hot` 직접 조회 (실시간) |
| **투표율 계산** | DB 기반 | Redis 기반 재계산 (실시간) |
| **갱신 주기** | 유저 액션 시 | 1분 TTL 캐싱 또는 실시간 |
| **실시간성** | 중간 (최대 5분 지연) | 높음 (최대 1분 지연) |

### 예상 성능

**응답 시간:**
- 캐시 히트: ~10ms
- 캐시 미스: ~100-300ms (Hot 투표 50개 기준)

**부하:**
- Redis 조회: Hot 투표당 1회 (totalParticipants)
- DB 조회: 1회 (Hot 투표 목록)

**권장 환경:**
- Redis: 적절한 메모리 및 연결 풀 설정
- DB: 인덱스 최적화 (VotePost.id)
- 모니터링: 응답 시간, 캐시 히트율 추적