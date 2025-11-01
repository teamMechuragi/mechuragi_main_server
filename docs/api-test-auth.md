# Swagger를 사용한 API 테스트 가이드

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

## 인증이 필요 없는 API 테스트

### 회원가입 플로우

**⚠️ 중요: 회원가입은 반드시 다음 순서로 진행해야 합니다!**

```
1. 이메일 인증 메일 발송 (POST /api/auth/email/send)
2. 이메일 인증 코드 확인 (POST /api/auth/email/verify)
3. 회원가입 (POST /api/auth/signup)
```

---

### 1. 이메일 인증 메일 발송 (POST /api/auth/email/send)

**요청 본문:**
```json
{
  "email": "test@example.com"
}
```

**응답:** 200 OK

**기능:**
- 이메일 중복 체크
- 6자리 인증 코드 생성 및 이메일 발송
- 인증 코드 유효기간: 30분

**참고:**
- 이미 사용 중인 이메일이면 400 에러 발생
- AWS SES로 이메일 발송 (Sandbox 모드에서는 인증된 이메일로만 발송 가능)

---

### 2. 이메일 인증 코드 확인 (POST /api/auth/email/verify)

**요청 본문:**
```json
{
  "email": "test@example.com",
  "verificationCode": "123456"
}
```

**응답:** 200 OK

**기능:**
- 인증 코드 일치 여부 확인
- 만료 여부 확인
- 인증 완료 처리

**참고:**
- 인증 코드가 일치하지 않으면 400 에러 발생
- 인증 코드가 만료되었으면 400 에러 발생
- 인증 완료 후에만 회원가입 가능

---

### 3. 랜덤 닉네임 생성 (GET /api/auth/nickname/generate)

**요청:** 없음

**응답:**
```json
{
  "nickname": "행복한곰"
}
```

**기능:**
- 랜덤 닉네임 생성 (형용사 + 명사 조합)
- 회원가입 시 닉네임 제안용

**참고:**
- 생성된 닉네임을 그대로 사용하거나 사용자가 수정 가능
- 최종 닉네임은 회원가입 시 자동으로 "닉네임 + 멤버ID" 형태로 저장됨 (예: "행복한곰1")

---

### 4. 회원가입 (POST /api/auth/signup)

**⚠️ 주의: 이메일 인증 완료 후에만 가능합니다!**

**요청 본문:**
```json
{
   "email": "twrp3301@gmail.com",
   "password": "abc123456!",
   "nickname": "행복한곰"
}
```

**응답:**
```json
{
  "id": 1,
  "email": "test@example.com",
  "nickname": "행복한곰1",
  "emailVerified": true,
  "provider": "NORMAL",
  "role": "USER",
  "status": "ACTIVE"
}
```

**기능:**
- 이메일 인증 완료 여부 확인
- 회원 정보 저장
- 닉네임 자동 변환 ("행복한곰" → "행복한곰1")
- 이메일 인증 정보 삭제

**참고:**
- 비밀번호는 8~20자, 영문/숫자/특수문자 포함 필요
- 닉네임은 자동으로 "입력한닉네임 + 멤버ID" 형태로 저장됨
- 이메일 인증이 완료되지 않았으면 400 에러 발생
- 이메일 인증이 만료되었으면 400 에러 발생

---

### 5. 로그인 (POST /api/auth/login)

**요청 본문:**
```json
{
   "email": "twrp3301@gmail.com",
   "password": "abc123456!"
}
```

**응답:**
```json
{
  "tokens": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400000
  },
  "member": {
    "id": 1,
    "email": "test@example.com",
    "nickname": "테스트유저",
    "emailVerified": false,
    "provider": "NORMAL",
    "role": "USER",
    "status": "ACTIVE"
  }
}
```

**중요:**
- `accessToken` 값을 복사해둡니다 (JWT 인증에 사용)
- Access Token 유효기간: 24시간
- Refresh Token 유효기간: 7일

---

### 6. 이메일 중복 체크 (GET /api/members/check/email)

**쿼리 파라미터:**
```
test@example.com
```

**응답:**
```json
true  // 중복됨
false // 사용 가능
```

**참고:**
- 회원가입 전 이메일 중복 체크용
- 이메일 인증 API에서도 자동으로 중복 체크를 수행하므로 선택적으로 사용 가능

---

### 7. 닉네임 중복 체크 (GET /api/members/check/nickname)

**쿼리 파라미터:**
```
테스트유저1
```

**응답:**
```json
true  // 중복됨
false // 사용 가능
```

**참고:**
- 회원가입 시 닉네임은 자동으로 "닉네임 + 멤버ID" 형태로 저장되므로 일반적으로 중복되지 않음
- 프로필 수정 시 닉네임 변경할 때 사용

---

## JWT 인증이 필요한 API 테스트

### JWT 토큰 설정 방법

1. **Swagger UI 우측 상단 "Authorize" 버튼 클릭**
2. **"Bearer Authentication" 섹션에 다음과 같이 입력:**
   ```
   Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
   ```
   (로그인 API에서 받은 accessToken 앞에 "Bearer " 붙임)
3. **"Authorize" 버튼 클릭**
4. **"Close" 버튼 클릭**

이제 JWT 인증이 필요한 API를 테스트할 수 있습니다!

---

### 8. 로그아웃 (POST /api/auth/logout)

**JWT 인증 필요** ✅

**요청:** 없음 (JWT 토큰으로 자동 인식)

**응답:** 204 No Content

**참고:**
- Refresh Token이 DB에서 삭제됨
- Access Token은 만료될 때까지 유효 (클라이언트에서 삭제 필요)

---

### 9. Access Token 재발급 (POST /api/auth/refresh)

**헤더:**
```
Refresh-Token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**응답:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400000
}
```

**참고:**
- Access Token이 만료되었을 때 사용
- Refresh Token이 유효하면 새로운 Access Token 발급

---

### 10. 회원 조회 (GET /api/members/{memberId})

**JWT 인증 필요** ✅

**경로 파라미터:**
```
memberId=1
```

**응답:**
```json
{
  "id": 1,
  "email": "test@example.com",
  "nickname": "테스트유저",
  "emailVerified": true,
  "provider": "NORMAL",
  "role": "USER",
  "status": "ACTIVE",
  "createdAt": "2025-10-15T10:30:00",
  "updatedAt": "2025-10-15T10:30:00"
}
```

---

### 11. 회원 정보 수정 (PUT /api/members/{memberId})

**JWT 인증 필요** ✅

**경로 파라미터:**
```
memberId=1
```

**요청 본문:**
```json
{
  "nickname": "새로운닉네임",
  "profileImageUrl": "https://example.com/profile.jpg"
}
```

**응답:**
```json
{
  "id": 1,
  "email": "test@example.com",
  "nickname": "새로운닉네임",
  "profileImageUrl": "https://example.com/profile.jpg",
  "emailVerified": true,
  "provider": "NORMAL",
  "role": "USER",
  "status": "ACTIVE"
}
```

**참고:**
- 닉네임 중복 시 에러 발생
- profileImageUrl은 선택사항

---

### 12. 비밀번호 변경 (PUT /api/members/{memberId}/password)

**JWT 인증 필요** ✅

**경로 파라미터:**
```
memberId=1
```

**요청 본문:**
```json
{
  "currentPassword": "Test1234!@",
  "newPassword": "NewTest1234!@"
}
```

**응답:** 200 OK

**참고:**
- 현재 비밀번호가 일치해야 함
- 소셜 로그인 회원은 비밀번호 변경 불가

---

### 13. 회원 탈퇴 (DELETE /api/members/{memberId})

**JWT 인증 필요** ✅

**경로 파라미터:**
```
memberId=1
```

**응답:** 204 No Content

**참고:**
- 소프트 삭제 (status가 INACTIVE로 변경)
- 실제 데이터는 DB에 남음

---

## 카카오 OAuth2 로그인 테스트

### 카카오 로그인 시작

**브라우저에서 직접 접속:**
```
http://localhost:8080/oauth2/authorization/kakao
```
또는
```
http://{baseurl}/oauth2/authorization/kakao
```

**흐름:**
1. 카카오 로그인 페이지로 리다이렉트
2. 카카오 계정으로 로그인
3. 동의 화면에서 권한 승인
4. 백엔드에서 JWT 토큰 발급
5. 프론트엔드로 리다이렉트:
   ```
   https://d4pdjk57v9eg4.cloudfront.net/oauth2/callback
   ?accessToken=...
   &refreshToken=...
   &tokenType=Bearer
   &expiresIn=86400000
   ```

**참고:**
- Swagger UI에서는 직접 테스트 불가 (브라우저 리다이렉트 필요)
- 카카오 로그인 성공 시 자동으로 회원 가입 또는 로그인 처리
- 소셜 로그인 회원은 emailVerified=true로 자동 설정
- **카카오에서 받는 정보:** 이메일만
- **자동 생성:** 닉네임 (랜덤닉네임 + 멤버ID, 예: "행복한곰1"), 프로필 이미지 (null)

---
## 에러 코드

### 400 Bad Request
- 필수 필드 누락
- 유효성 검증 실패 (이메일 형식, 비밀번호 규칙 등)

### 401 Unauthorized
- JWT 토큰이 없거나 유효하지 않음
- 비밀번호가 일치하지 않음

### 403 Forbidden
- 권한이 없음 (예: 다른 회원의 정보 수정 시도)

### 409 Conflict
- 이메일 또는 닉네임 중복

---
