# AI 추천 및 스크랩 API 테스트 가이드

## Swagger UI 접속

서버 실행 후 다음 URL로 접속:

### 로컬 환경
```
http://localhost:8080/swagger-ui/index.html
```

### 프로덕션 환경
```
http://{baseURL}:8080/swagger-ui/index.html
```

---

## JWT 인증 설정 (필수)

**⚠️ 모든 AI API는 JWT 인증이 필요합니다!**

### JWT 토큰 설정 방법

1. **먼저 로그인 API를 통해 Access Token을 발급받습니다**
   - `POST /api/auth/login` 참고 (api-test-auth.md 참고)

2. **Swagger UI 우측 상단 "Authorize" 버튼 클릭**

3. **"Bearer Authentication" 섹션에 다음과 같이 입력:**
   ```
   Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
   ```
   (로그인 API에서 받은 accessToken 앞에 "Bearer " 붙임)

4. **"Authorize" 버튼 클릭**

5. **"Close" 버튼 클릭**

이제 AI API를 테스트할 수 있습니다!

---

## AI 음식 추천 API (AiRecommendationController)

### 1. 날씨 기반 음식 추천 (POST /api/ai-recommendations/weather)

**JWT 인증 필요** ✅

**요청 본문:**
```json
{
  "weatherConditions": ["맑음", "따뜻함"]
}
```

**응답:**
```json
{
  "message": "날씨에 맞는 음식을 추천해드렸습니다.",
  "recommendations": [
    {
      "recommendationType": "WEATHER",
      "name": "냉면",
      "description": "시원한 육수와 함께 즐기는 여름 별미",
      "reason": "따뜻하고 맑은 날씨에는 시원한 냉면이 제격입니다",
      "ingredients": "메밀면, 육수, 오이, 계란 등",
      "cookingTime": "30분",
      "difficulty": "중"
    }
  ],
  "model": "gpt-4o-mini"
}
```

**기능:**
- 현재 날씨 조건에 맞는 음식 추천
- 사용자 선호도 반영

**참고:**
- weatherConditions는 배열 형태로 여러 조건 입력 가능
- 예시: ["비", "쌀쌀함"], ["맑음", "더움"] 등

---

### 2. 시간대 기반 음식 추천 (POST /api/ai-recommendations/time)

**JWT 인증 필요** ✅

**요청 본문:**
```json
{
  "timeOfDay": "저녁"
}
```

**응답:**
```json
{
  "message": "저녁 시간에 어울리는 음식을 추천해드렸습니다.",
  "recommendations": [
    {
      "recommendationType": "TIME_BASED",
      "name": "삼겹살",
      "description": "고소한 돼지고기를 구워 먹는 한국식 바베큐",
      "reason": "저녁 시간에 가족, 친구들과 함께 즐기기 좋습니다",
      "ingredients": "삼겹살, 상추, 쌈장, 마늘 등",
      "cookingTime": "20분",
      "difficulty": "하"
    }
  ],
  "model": "gpt-4o-mini"
}
```

**기능:**
- 아침, 점심, 저녁 등 시간대에 맞는 음식 추천
- 사용자 선호도 반영

**참고:**
- timeOfDay 예시: "아침", "점심", "저녁", "야식" 등

---

### 3. 재료 기반 음식 추천 (POST /api/ai-recommendations/ingredients)

**JWT 인증 필요** ✅

**요청 본문:**
```json
{
  "ingredients": ["계란", "양파", "감자"]
}
```

**응답:**
```json
{
  "message": "보유하신 재료로 만들 수 있는 음식을 추천해드렸습니다.",
  "recommendations": [
    {
      "recommendationType": "INGREDIENTS",
      "name": "스페니시 오믈렛",
      "description": "계란과 감자로 만드는 스페인식 오믈렛",
      "reason": "가지고 계신 재료로 쉽게 만들 수 있는 영양 만점 요리입니다",
      "ingredients": "계란 4개, 감자 2개, 양파 1개, 소금, 후추, 올리브유",
      "cookingTime": "25분",
      "difficulty": "중"
    }
  ],
  "model": "gpt-4o-mini"
}
```

**기능:**
- 보유한 재료로 만들 수 있는 음식 추천
- 사용자 선호도 반영

**참고:**
- ingredients는 배열 형태로 여러 재료 입력 가능
- 냉장고에 있는 재료를 활용한 음식 추천

---

### 4. 기분 기반 음식 추천 (POST /api/ai-recommendations/feeling)

**JWT 인증 필요** ✅

**요청 본문:**
```json
{
  "feeling": "우울함"
}
```

**응답:**
```json
{
  "message": "기분 전환에 도움이 되는 음식을 추천해드렸습니다.",
  "recommendations": [
    {
      "recommendationType": "FEELING",
      "name": "치킨",
      "description": "바삭한 튀김옷의 프라이드 치킨",
      "reason": "맛있는 치킨으로 기분을 UP 시켜보세요!",
      "ingredients": "닭고기, 튀김가루, 양념 등",
      "cookingTime": "40분",
      "difficulty": "중상"
    }
  ],
  "model": "gpt-4o-mini"
}
```

**기능:**
- 현재 기분에 맞는 음식 추천
- 감정에 따른 맞춤 추천

**참고:**
- feeling 예시: "행복함", "우울함", "피곤함", "스트레스" 등

---

### 5. 대화 기반 음식 추천 (POST /api/ai-recommendations/conversation)

**JWT 인증 필요** ✅

**요청 본문:**
```json
{
  "message": "오늘 친구들이랑 모임인데 뭐 먹을까요?"
}
```

**응답:**
```json
{
  "message": "모임에 어울리는 음식을 추천해드렸습니다.",
  "recommendations": [
    {
      "recommendationType": "CONVERSATION",
      "name": "족발",
      "description": "쫄깃한 돼지족발에 보쌈김치를 곁들인 요리",
      "reason": "여러 명이 함께 나눠 먹기 좋고, 술안주로도 훌륭합니다",
      "ingredients": "족발, 보쌈김치, 쌈 채소 등",
      "cookingTime": "2시간",
      "difficulty": "상"
    }
  ],
  "model": "gpt-4o-mini"
}
```

**기능:**
- 자연어로 상황을 설명하면 AI가 맥락을 파악하여 추천
- 가장 유연한 추천 방식

**참고:**
- 자유로운 형식으로 요청 가능
- 상황, 인원, 선호도 등을 자연스럽게 설명

---

## 스크랩 관리 API (ScrapedFoodController)

### 6. 음식 추천 스크랩 저장 (POST /api/scraped-foods)

**JWT 인증 필요** ✅

**요청 본문:**
```json
{
  "recommendationType": "WEATHER",
  "name": "냉면",
  "description": "시원한 육수와 함께 즐기는 여름 별미",
  "reason": "따뜻하고 맑은 날씨에는 시원한 냉면이 제격입니다",
  "ingredients": "메밀면, 육수, 오이, 계란 등",
  "cookingTime": "30분",
  "difficulty": "중"
}
```

**응답:** (HTTP 201 Created)
```json
{
  "id": 1,
  "recommendationType": "WEATHER",
  "name": "냉면",
  "description": "시원한 육수와 함께 즐기는 여름 별미",
  "reason": "따뜻하고 맑은 날씨에는 시원한 냉면이 제격입니다",
  "ingredients": "메밀면, 육수, 오이, 계란 등",
  "cookingTime": "30분",
  "difficulty": "중",
  "createdAt": "2025-11-01T10:30:00"
}
```

**기능:**
- AI 추천 결과를 스크랩하여 저장
- 나중에 다시 확인 가능

**참고:**
- recommendationType: WEATHER, TIME_BASED, INGREDIENTS, FEELING, CONVERSATION 중 하나
- 모든 필드가 선택사항이지만, name은 필수 권장

---

### 7. 스크랩 목록 조회 (GET /api/scraped-foods)

**JWT 인증 필요** ✅

**요청:** 없음 (JWT 토큰으로 자동 인식)

**응답:**
```json
[
  {
    "id": 1,
    "recommendationType": "WEATHER",
    "name": "냉면",
    "description": "시원한 육수와 함께 즐기는 여름 별미",
    "reason": "따뜻하고 맑은 날씨에는 시원한 냉면이 제격입니다",
    "ingredients": "메밀면, 육수, 오이, 계란 등",
    "cookingTime": "30분",
    "difficulty": "중",
    "createdAt": "2025-11-01T10:30:00"
  },
  {
    "id": 2,
    "recommendationType": "INGREDIENTS",
    "name": "김치찌개",
    "description": "얼큰한 김치찌개",
    "reason": "집에 있는 김치로 간단하게 만들 수 있습니다",
    "ingredients": "김치, 돼지고기, 두부, 파 등",
    "cookingTime": "20분",
    "difficulty": "하",
    "createdAt": "2025-11-01T12:00:00"
  }
]
```

**기능:**
- 내가 저장한 모든 스크랩 목록 조회
- 최신순으로 정렬되어 반환

**참고:**
- 본인의 스크랩만 조회 가능

---

### 8. 스크랩 상세 조회 (GET /api/scraped-foods/{scrapId})

**JWT 인증 필요** ✅

**경로 파라미터:**
```
scrapId=1
```

**응답:**
```json
{
  "id": 1,
  "recommendationType": "WEATHER",
  "name": "냉면",
  "description": "시원한 육수와 함께 즐기는 여름 별미",
  "reason": "따뜻하고 맑은 날씨에는 시원한 냉면이 제격입니다",
  "ingredients": "메밀면, 육수, 오이, 계란 등",
  "cookingTime": "30분",
  "difficulty": "중",
  "createdAt": "2025-11-01T10:30:00"
}
```

**기능:**
- 특정 스크랩의 상세 정보 조회

**참고:**
- 본인의 스크랩만 조회 가능 (다른 사용자의 스크랩 조회 시 403 에러)

---

### 9. 스크랩 삭제 (DELETE /api/scraped-foods/{scrapId})

**JWT 인증 필요** ✅

**경로 파라미터:**
```
scrapId=1
```

**응답:** HTTP 204 No Content

**기능:**
- 저장된 스크랩 삭제

**참고:**
- 본인의 스크랩만 삭제 가능 (다른 사용자의 스크랩 삭제 시 403 에러)
- 삭제된 스크랩은 복구 불가능

---

## 일반적인 사용 플로우

### 추천 → 스크랩 → 확인 플로우

1. **AI 추천 받기**
   - 원하는 추천 API 선택 (날씨/시간/재료/기분/대화)
   - 추천 결과 확인

2. **마음에 드는 추천 스크랩하기**
   - `POST /api/scraped-foods`로 추천 결과 저장
   - AI 응답의 recommendations 배열에서 원하는 항목을 선택하여 저장

3. **스크랩 목록 확인**
   - `GET /api/scraped-foods`로 저장된 스크랩 목록 조회

4. **상세 정보 확인**
   - `GET /api/scraped-foods/{scrapId}`로 특정 스크랩 상세 조회

5. **필요 없는 스크랩 삭제**
   - `DELETE /api/scraped-foods/{scrapId}`로 스크랩 삭제

---

## 에러 코드

### 400 Bad Request
- 필수 필드 누락
- 잘못된 요청 형식

### 401 Unauthorized
- JWT 토큰이 없거나 유효하지 않음
- 로그인 필요

### 403 Forbidden
- 다른 사용자의 스크랩 접근 시도
- 권한 없음

### 404 Not Found
- 존재하지 않는 스크랩 ID
- 리소스를 찾을 수 없음

### 500 Internal Server Error
- AI API 호출 실패
- 서버 내부 오류

---

## 추천 타입 (RecommendationType)

- `WEATHER`: 날씨 기반 추천
- `TIME_BASED`: 시간대 기반 추천
- `INGREDIENTS`: 재료 기반 추천
- `FEELING`: 기분 기반 추천
- `CONVERSATION`: 대화 기반 추천

---

## 참고 사항

- 모든 AI 추천 API는 사용자의 음식 선호도 정보를 자동으로 반영합니다
- AI 모델은 `gpt-4o-mini`를 사용합니다
- 추천 결과는 실시간으로 생성되며 저장되지 않습니다 (스크랩 필요)
- 스크랩은 사용자별로 독립적으로 관리됩니다
- JWT 토큰 유효기간: 24시간 (만료 시 재로그인 필요)

### AI 추천 vs 스크랩 차이점

| 구분 | AI 추천 (FoodRecommendationDto) | 스크랩 (ScrapedFoodResponse) |
|------|--------------------------------|------------------------------|
| **id** | ❌ 없음 (아직 저장 안됨) | ✅ 있음 (DB 저장된 ID) |
| **createdAt** | ❌ 없음 | ✅ 있음 (저장 시각) |
| **용도** | AI 실시간 추천 응답 | 사용자가 저장한 스크랩 |
| **API** | POST /api/ai-recommendations/* | GET/POST/DELETE /api/scraped-foods |

---
