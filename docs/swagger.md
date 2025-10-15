# Swagger를 사용한 API 테스트 가이드

## Swagger UI 접속

서버 실행 후 다음 URL로 접속:

### 로컬 환경
```
http://localhost:8080/swagger-ui/index.html
```

### 프로덕션 환경
```
http://15.165.136.100:8081/swagger-ui/index.html
```

## 인증이 필요 없는 API 테스트

### 1. 회원가입 (POST /api/auth/signup)

**요청 본문:**
```json
{
  "email": "test@example.com",
  "password": "Test1234!@",
  "nickname": "테스트유저"
}
```

**응답:**
```json
{
  "id": 1,
  "email": "test@example.com",
  "nickname": "테스트유저",
  "emailVerified": false,
  "provider": "NORMAL",
  "role": "USER",
  "status": "ACTIVE"
}
```

**참고:**
- 비밀번호는 8~20자, 영문/숫자/특수문자 포함 필요
- 닉네임은 2~20자, 한글/영문/숫자만 가능
- 회원가입 시 이메일 인증 메일 자동 발송

---

### 2. 로그인 (POST /api/auth/login)

**요청 본문:**
```json
{
  "email": "test@example.com",
  "password": "Test1234!@"
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

### 3. 이메일 중복 체크 (GET /api/members/check/email)

**쿼리 파라미터:**
```
email=test@example.com
```

**응답:**
```json
true  // 중복됨
false // 사용 가능
```

---

### 4. 닉네임 중복 체크 (GET /api/members/check/nickname)

**쿼리 파라미터:**
```
nickname=테스트유저
```

**응답:**
```json
true  // 중복됨
false // 사용 가능
```

---

### 5. 이메일 인증 메일 발송 (POST /api/auth/email/send)

**요청 본문:**
```json
{
  "email": "test@example.com"
}
```

**응답:** 200 OK

**참고:**
- 6자리 인증 코드가 이메일로 발송됨
- 인증 코드 유효기간: 30분

---

### 6. 이메일 인증 코드 확인 (POST /api/auth/email/verify)

**요청 본문:**
```json
{
  "email": "test@example.com",
  "verificationCode": "123456"
}
```

**응답:** 200 OK

**참고:**
- 인증 성공 시 Member.emailVerified가 true로 변경됨

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

### 7. 로그아웃 (POST /api/auth/logout)

**JWT 인증 필요** ✅

**요청:** 없음 (JWT 토큰으로 자동 인식)

**응답:** 204 No Content

**참고:**
- Refresh Token이 DB에서 삭제됨
- Access Token은 만료될 때까지 유효 (클라이언트에서 삭제 필요)

---

### 8. Access Token 재발급 (POST /api/auth/refresh)

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

### 9. 회원 조회 (GET /api/members/{memberId})

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

### 10. 회원 정보 수정 (PUT /api/members/{memberId})

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

### 11. 비밀번호 변경 (PUT /api/members/{memberId}/password)

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

### 12. 회원 탈퇴 (DELETE /api/members/{memberId})

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
http://15.165.136.100:8080/oauth2/authorization/kakao
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

---

## 테스트 시나리오

### 시나리오 1: 일반 회원가입 → 이메일 인증 → 로그인

1. **회원가입** (POST /api/auth/signup)
2. **이메일 인증 메일 확인** (받은 6자리 코드 확인)
3. **이메일 인증** (POST /api/auth/email/verify)
4. **로그인** (POST /api/auth/login)
5. **Access Token 복사 → Authorize 설정**
6. **회원 정보 조회** (GET /api/members/{memberId})

### 시나리오 2: 회원 정보 수정 → 비밀번호 변경

1. **로그인** (Access Token 발급)
2. **Authorize 설정**
3. **회원 정보 수정** (PUT /api/members/{memberId})
4. **비밀번호 변경** (PUT /api/members/{memberId}/password)
5. **로그아웃** (POST /api/auth/logout)
6. **새 비밀번호로 재로그인**

### 시나리오 3: 토큰 재발급

1. **로그인** (Access Token, Refresh Token 발급)
2. **24시간 후 Access Token 만료**
3. **토큰 재발급** (POST /api/auth/refresh, Refresh-Token 헤더에 Refresh Token 전달)
4. **새로운 Access Token으로 API 사용**

### 시나리오 4: 카카오 로그인

1. **브라우저에서 카카오 로그인** (GET /oauth2/authorization/kakao)
2. **카카오 계정 로그인 및 권한 승인**
3. **프론트엔드로 리다이렉트되면서 JWT 토큰 수령**
4. **Access Token을 Swagger Authorize에 설정**
5. **인증이 필요한 API 테스트**

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

## 주의사항

### JWT 토큰 설정
- Swagger UI에서 "Authorize" 버튼으로 JWT 토큰 설정
- 토큰 앞에 **"Bearer "** 문자열 포함 필수
- 예: `Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`

### 환경별 서버 URL
- **로컬:** http://localhost:8080
- **프로덕션:** http://15.165.136.100:8080
- Swagger UI 우측 상단에서 서버 선택 가능

### CORS 설정
- 허용된 Origin:
  - http://localhost:3000
  - http://localhost:8080
  - https://d4pdjk57v9eg4.cloudfront.net

### 카카오 OAuth2 설정
- **카카오 개발자 콘솔에 Redirect URI 등록 필수:**
  - http://15.165.136.100:8080/login/oauth2/code/kakao

---

## 추가 리소스

- **OpenAPI 문서 JSON:** http://localhost:8080/v3/api-docs
- **Actuator Health Check:** http://localhost:8080/actuator/health
- **프로젝트 인증 흐름 문서:** docs/auth.md
