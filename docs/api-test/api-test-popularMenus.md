# 실시간 인기 메뉴 API 테스트 가이드

## 📋 Swagger UI 접근

```
http://localhost:8080/swagger-ui/index.html
```

**API 위치:** `vote-controller` > `GET /api/votes/popular-menus`

---

## 📦 테스트 데이터 준비

### 1️⃣ 정규화 테스트용 투표 생성

**Swagger에서:**
1. `POST /api/votes` 클릭
2. "Try it out" 클릭
3. 아래 JSON 입력 후 "Execute"

```json
{
  "title": "점심 메뉴 추천 (정규화 테스트)",
  "description": "오늘 점심 뭐 먹을까요?",
  "deadline": "2025-12-31T23:59:59",
  "allowMultipleChoice": false,
  "options": [
    {
      "optionText": "🍜 마라탕!!!",
      "imageUrl": null
    },
    {
      "optionText": "마 라 탕",
      "imageUrl": null
    },
    {
      "optionText": "마라탕",
      "imageUrl": null
    },
    {
      "optionText": "김치찌개",
      "imageUrl": null
    }
  ]
}
```

---

### 2️⃣ 동의어 테스트용 투표 생성 (파스타)

```json
{
  "title": "저녁 메뉴 추천 (동의어 1)",
  "description": "이탈리안 메뉴",
  "deadline": "2025-12-31T23:59:59",
  "allowMultipleChoice": false,
  "options": [
    {
      "optionText": "파스타",
      "imageUrl": null
    },
    {
      "optionText": "피자",
      "imageUrl": null
    },
    {
      "optionText": "리조또",
      "imageUrl": null
    }
  ]
}
```

---

### 3️⃣ 동의어 테스트용 투표 생성 (스파게티)

```json
{
  "title": "저녁 메뉴 추천 (동의어 2)",
  "description": "이탈리안 메뉴 2",
  "deadline": "2025-12-31T23:59:59",
  "allowMultipleChoice": false,
  "options": [
    {
      "optionText": "스파게티",
      "imageUrl": null
    },
    {
      "optionText": "pizza",
      "imageUrl": null
    },
    {
      "optionText": "샐러드",
      "imageUrl": null
    }
  ]
}
```

---

### 4️⃣ 일반 메뉴 투표 1

```json
{
  "title": "오늘 저녁 메뉴는?",
  "description": "인기 메뉴 투표",
  "deadline": "2025-12-31T23:59:59",
  "allowMultipleChoice": false,
  "options": [
    {
      "optionText": "치킨",
      "imageUrl": null
    },
    {
      "optionText": "burger",
      "imageUrl": null
    },
    {
      "optionText": "떡볶이",
      "imageUrl": null
    },
    {
      "optionText": "김밥",
      "imageUrl": null
    }
  ]
}
```

---

### 5️⃣ 일반 메뉴 투표 2

```json
{
  "title": "야식 메뉴 추천",
  "description": "밤에 먹으면 맛있는 메뉴",
  "deadline": "2025-12-31T23:59:59",
  "allowMultipleChoice": false,
  "options": [
    {
      "optionText": "라면",
      "imageUrl": null
    },
    {
      "optionText": "chicken",
      "imageUrl": null
    },
    {
      "optionText": "짜장면",
      "imageUrl": null
    },
    {
      "optionText": "햄버거",
      "imageUrl": null
    }
  ]
}
```

---

### 6️⃣ 한식 메뉴 투표

```json
{
  "title": "한식 메뉴 뭐 먹을까?",
  "description": "한식 메뉴 추천",
  "deadline": "2025-12-31T23:59:59",
  "allowMultipleChoice": false,
  "options": [
    {
      "optionText": "김치찌개",
      "imageUrl": null
    },
    {
      "optionText": "된장찌개",
      "imageUrl": null
    },
    {
      "optionText": "불고기",
      "imageUrl": null
    },
    {
      "optionText": "돈까스",
      "imageUrl": null
    }
  ]
}
```

---

### 📝 투표 생성 후 필수 작업

각 투표를 생성한 후, **반드시 투표에 참여하고 좋아요**를 눌러야 Hot 투표가 됩니다!

#### 1. 투표 참여
`POST /api/votes/participate` 사용

```json
{
  "voteId": 1,  // 생성된 투표 ID
  "optionIds": [1]  // 선택한 옵션 ID
}
```

#### 2. 좋아요
`POST /api/votes/{voteId}/like` 사용

---

## 🎯 테스트 시나리오

### 시나리오 1: 기본 조회 테스트

**전제 조건:**
- 위의 6개 투표가 모두 생성됨
- 각 투표에 1회 이상 참여 및 좋아요

**Swagger에서:**
1. `GET /api/votes/popular-menus` 클릭
2. "Try it out" 클릭
3. "Execute" 클릭

**검증:**
- ✅ Status: 200
- ✅ Body: 배열 (최대 10개)
- ✅ `score` 내림차순 정렬

---

### 시나리오 2: 메뉴명 정규화 테스트

**전제 조건:**
- 투표 1번 (정규화 테스트용) 생성 및 참여

**예상 결과:**
```json
{
  "menu": "마라탕",
  "mentionCount": 3,  // "🍜 마라탕!!!", "마 라 탕", "마라탕" 통합
  ...
}
```

---

### 시나리오 3: 동의어 처리 테스트

**전제 조건:**
- 투표 2번, 3번 (동의어 테스트용) 생성 및 참여

**예상 결과:**
```json
[
  {
    "menu": "스파게티",
    "mentionCount": 2,  // "파스타" + "스파게티" 통합
    ...
  },
  {
    "menu": "pizza",
    "mentionCount": 2,  // "피자" + "pizza" 통합
    ...
  }
]
```

**동의어 매핑:**
- 파스타 → 스파게티
- 짜장면 → 자장면
- 라면 → 라멘
- 돈까스 → 돈카츠
- 떡볶이 → 떡복이
- 김밥, gimbap → kimbap
- 치킨 → chicken
- 피자 → pizza
- 햄버거, 버거 → burger

---

### 시나리오 4: 점수 계산 검증

**점수 공식:**
```
score = (mentionCount × 10.0)
      + (averageVotePercentage × 0.5)
      + (averageRecency × 2.0)
      + 1.0
```

**검증 예시:**
```javascript
// 응답 데이터
mentionCount = 2
averageVotePercentage = 37.5
averageRecency = 0.714

// 계산
expectedScore = (2 × 10.0) + (37.5 × 0.5) + (0.714 × 2.0) + 1.0
              = 20 + 18.75 + 1.428 + 1.0
              = 41.178 ✅
```

---

### 시나리오 5: 실시간성 테스트 (1분 캐싱)

**테스트 순서:**
1. Swagger에서 인기 메뉴 조회 → 결과 저장
2. 새로운 투표 생성 및 참여 (Hot 투표 영향)
3. 30초 후 다시 조회 → **같은 결과** (캐시 히트)
4. 70초 후 다시 조회 → **다른 결과** (캐시 만료, 재계산)

---

## ✅ 검증 체크리스트

### 기본 검증
- [ ] Status: 200 OK
- [ ] 배열 크기 ≤ 10
- [ ] 5개 필드 존재 (`menu`, `score`, `mentionCount`, `averageVotePercentage`, `averageRecency`)

### 비즈니스 로직 검증
- [ ] `score` 내림차순 정렬
- [ ] `mentionCount` ≥ 1
- [ ] `averageVotePercentage` 범위: 0~100
- [ ] `averageRecency` 범위: 0~1
- [ ] 정규화 적용 (공백/특수문자/이모지 제거)
- [ ] 동의어 처리 적용
- [ ] 점수 계산 정확성
- [ ] 1분 캐싱 작동

---

## 🐛 트러블슈팅

### 빈 배열 반환 `[]`

**원인:**
1. Hot 투표 없음 → Swagger에서 `GET /api/votes/hot` 확인
2. Redis `vote:hot` 키 없음 → 서버 재시작
3. 모든 메뉴명 필터링됨 → 로그 확인

---

### 동일 메뉴 중복 표시

**원인:** 정규화 로직 오류

**확인:**
```java
MenuNormalizer.normalize("마 라 탕") // "마라탕" 반환해야 함
```

---

### 점수 비정상

**확인:**
1. 로그: `인기 메뉴 계산 완료...`
2. Redis: `redis-cli GET vote:1:participants`
3. 수동 점수 계산 후 비교

---

### 캐싱 미작동

**증상:** 매 요청마다 로그 출력

**확인:**
```bash
redis-cli PING  # PONG 응답 확인
redis-cli KEYS *popularMenus*
```

---

## 📊 예상 성능

| 상황 | 응답 시간 |
|------|----------|
| 캐시 히트 | ~10-50ms |
| 캐시 미스 | ~100-300ms |

---

## 🔗 관련 API

- **Hot 투표 조회**: `GET /api/votes/hot`
- **투표 참여**: `POST /api/votes/participate`
- **설계 문서**: [realTime-popularMenus.md](../logic/popular-menus.md)
