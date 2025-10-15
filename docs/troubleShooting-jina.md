# API 테스트 및 문제 해결 로그

## 테스트 일자: 2025-10-15

---

## 테스트 환경
- 로컬 서버: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- 브랜치: feat/auth

---

## 테스트 진행 상황

### 1. 회원가입 (POST /api/auth/signup)
**상태:** ✅ 성공

---

### 2. 로그인 (POST /api/auth/login)
**상태:** ✅ 성공

---

### 3. 이메일 중복 체크 (GET /api/members/check/email)
**상태:** ✅ 성공

---

### 4. 닉네임 중복 체크 (GET /api/members/check/nickname)
**상태:** ✅ 성공

---

### 5. 이메일 인증 메일 발송 (POST /api/auth/email/send)
**상태:** ❌ 실패 (500 에러)

---

### 6. 이메일 인증 코드 확인 (POST /api/auth/email/verify)
**상태:** ⏭️ 건너뜀 (이메일 발송 실패로 인증 코드 없음)

---

### 7. 로그아웃 (POST /api/auth/logout)
**상태:** ✅ 성공

---

### 8. Access Token 재발급 (POST /api/auth/refresh)
**상태:** ✅ 성공 (로그인 상태에서 재테스트 후 성공)

---

### 9. 회원 조회 (GET /api/members/{memberId})
**상태:** ✅ 성공

---

### 10. 회원 정보 수정 (PUT /api/members/{memberId})
**상태:** ✅ 성공

---

### 11. 비밀번호 변경 (PUT /api/members/{memberId}/password)
**상태:** ✅ 성공

---

### 12. 회원 탈퇴 (DELETE /api/members/{memberId})
**상태:** ✅ 성공

---

## 문제 해결 내역

### 문제 1: 회원가입 API 500 에러 - Transaction Rollback 에러
**발생 일시:** 2025-10-15 14:10

**에러 메시지:**
```
org.springframework.transaction.UnexpectedRollbackException: Transaction silently rolled back because it has been marked as rollback-only
```

**원인 분석:**
1. `AuthService.signup()` 메서드가 `@Transactional`로 선언됨
2. 내부에서 `EmailService.sendVerificationEmail()` 호출 시 AWS SES 이메일 발송 실패
3. `EmailService.sendVerificationEmail()`도 `@Transactional`로 선언되어 있어 같은 트랜잭션 내에서 실행됨
4. 이메일 발송 실패 시 `RuntimeException`이 발생하여 트랜잭션이 롤백 마크됨
5. `AuthService.signup()`에서 try-catch로 예외를 잡았지만, 이미 트랜잭션이 롤백 마크되어 커밋 불가
6. 결과적으로 회원 저장도 롤백됨

**해결 방법:**
회원가입 로직에서 이메일 발송 로직을 완전히 분리
- `AuthService.signup()` 메서드에서 `emailService.sendVerificationEmail()` 호출 코드 제거
- 회원가입과 이메일 인증 메일 발송을 독립적인 API로 분리
- 클라이언트에서 회원가입 후 별도로 이메일 인증 메일 발송 API (POST /api/auth/email/send) 호출

**수정 파일:**
- `src/main/java/com/mechuragi/mechuragi_server/auth/service/AuthService.java:34-64`
  - 이메일 발송 관련 코드 제거 (try-catch 블록 포함)
  - `EmailService` 의존성 제거

**장점:**
- 트랜잭션 경계가 명확해짐
- 회원가입과 이메일 발송이 독립적으로 실패/성공 가능
- 이메일 발송 실패 시 클라이언트에서 명확하게 에러 처리 가능
- 사용자가 원하는 시점에 인증 메일 재발송 가능

**API 사용 흐름:**
1. 회원가입: POST /api/auth/signup
2. 이메일 인증 메일 발송: POST /api/auth/email/send
3. 이메일 인증: POST /api/auth/email/verify

---

### 문제 2: 이메일 인증 메일 발송 API 500 에러 - AWS SES 발송 실패
**발생 일시:** 2025-10-15 14:29

**에러 메시지:**
```
java.lang.RuntimeException: 이메일 발송에 실패했습니다.
at com.mechuragi.mechuragi_server.auth.service.EmailService.sendVerificationEmail(EmailService.java:75)
```

**원인 분석:**
AWS SES 이메일 발송 실패. 가능한 원인:

1. **AWS SES Sandbox 모드**
   - SES가 Sandbox 모드인 경우 인증된 이메일 주소로만 발송 가능
   - 테스트 중인 수신자 이메일이 SES에서 인증되지 않음

2. **발신자 이메일 미인증**
   - `application-local.yml`의 `SES_FROM_EMAIL` (mechuragi001@gmail.com)이 SES에서 인증되지 않음
   - SES에서 발신자 이메일 또는 도메인 인증 필요

3. **AWS 자격 증명 문제**
   - `.env` 파일의 AWS 자격 증명(AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY) 누락 또는 잘못됨
   - 또는 IAM 권한에 SES 발송 권한 없음

4. **리전 불일치**
   - SES 발신자 이메일이 인증된 리전과 코드의 리전이 다름
   - 현재 설정: ap-northeast-2 (서울)

**해결 방법:**

#### 방법 1: AWS SES Sandbox 모드 확인 및 수신자 이메일 인증
```bash
# AWS Console에서 확인
1. AWS SES Console 접속
2. Account dashboard에서 Sandbox 상태 확인
3. Sandbox 모드인 경우:
   - Verified identities에서 수신자 이메일 추가 및 인증
   - 또는 Production access 신청
```

#### 방법 2: 발신자 이메일 인증
```bash
# AWS SES에서 이메일 인증
1. AWS SES Console → Verified identities
2. Create identity 클릭
3. Email address 선택
4. mechuragi001@gmail.com 입력
5. 받은 인증 이메일에서 링크 클릭하여 인증 완료
```

#### 방법 3: AWS 자격 증명 확인
```bash
# .env 파일 확인
AWS_ACCESS_KEY_ID=your-access-key
AWS_SECRET_ACCESS_KEY=your-secret-key
AWS_REGION=ap-northeast-2

# IAM 권한 확인
- AmazonSESFullAccess 또는 ses:SendEmail 권한 필요
```

#### 방법 4: 임시 해결 - 이메일 발송 로직 비활성화
테스트를 위해 이메일 발송 없이 인증 코드만 로그에 출력하도록 수정 가능

**현재 상태:**
- AWS SES 설정 확인 및 인증 필요
- 테스트 계속을 위해 이메일 발송 API는 건너뛰고 다른 API 테스트 진행 가능

**참고:**
- SES Sandbox 모드에서는 하루 200개 이메일, 초당 1개 제한
- Production 모드로 전환 시 AWS 승인 필요 (1-2일 소요)

---

### 문제 3: Access Token 재발급 API 500 에러 - Refresh Token을 찾을 수 없음
**발생 일시:** 2025-10-15 14:55

**에러 메시지:**
```
java.lang.IllegalArgumentException: Refresh Token을 찾을 수 없습니다.
at com.mechuragi.mechuragi_server.auth.service.JwtService.lambda$refreshAccessToken$2(JwtService.java:79)
```

**원인 분석:**
테스트 순서 문제
- 로그아웃 API(7번)를 먼저 테스트한 후 Refresh Token API(8번)를 테스트함
- 로그아웃 시 `JwtService.logout()` 메서드가 `refreshTokenRepository.deleteByMemberId()`를 호출하여 DB에서 Refresh Token 삭제
- 삭제된 Refresh Token으로 재발급 시도 → "Refresh Token을 찾을 수 없습니다" 에러

**해결 방법:**
올바른 테스트 순서로 재테스트
```
1. 로그인 (POST /api/auth/login) → Refresh Token 받음
2. Refresh Token 재발급 (POST /api/auth/refresh) 테스트
3. 로그아웃 (POST /api/auth/logout) 테스트
```

**결과:**
로그인 상태에서 재테스트 → ✅ 성공

**교훈:**
- 로그아웃은 Refresh Token을 삭제하므로 재발급 테스트 후에 실행해야 함
- API 테스트 시 의존성과 순서를 고려해야 함

---

## 참고사항
- JWT 토큰은 "Bearer " 접두사 필요
- Access Token 유효기간: 24시간
- Refresh Token 유효기간: 7일
- 비밀번호 규칙: 8~20자, 영문/숫자/특수문자 포함
- 닉네임 규칙: 2~20자, 한글/영문/숫자만 가능
