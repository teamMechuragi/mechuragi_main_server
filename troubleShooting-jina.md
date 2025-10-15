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
**상태:** 대기 중

---

### 2. 로그인 (POST /api/auth/login)
**상태:** 대기 중

---

### 3. 이메일 중복 체크 (GET /api/members/check/email)
**상태:** 대기 중

---

### 4. 닉네임 중복 체크 (GET /api/members/check/nickname)
**상태:** 대기 중

---

### 5. 이메일 인증 메일 발송 (POST /api/auth/email/send)
**상태:** 대기 중

---

### 6. 이메일 인증 코드 확인 (POST /api/auth/email/verify)
**상태:** 대기 중

---

### 7. 로그아웃 (POST /api/auth/logout)
**상태:** 대기 중

---

### 8. Access Token 재발급 (POST /api/auth/refresh)
**상태:** 대기 중

---

### 9. 회원 조회 (GET /api/members/{memberId})
**상태:** 대기 중

---

### 10. 회원 정보 수정 (PUT /api/members/{memberId})
**상태:** 대기 중

---

### 11. 비밀번호 변경 (PUT /api/members/{memberId}/password)
**상태:** 대기 중

---

### 12. 회원 탈퇴 (DELETE /api/members/{memberId})
**상태:** 대기 중

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
`EmailService.sendVerificationEmail()` 메서드에 `@Transactional(propagation = Propagation.REQUIRES_NEW)` 적용
- 이메일 발송을 별도의 새로운 트랜잭션으로 분리
- 이메일 발송이 실패해도 회원가입 트랜잭션에는 영향을 주지 않음
- 회원가입은 성공하고, 이메일은 나중에 재발송 가능

**수정 파일:**
- `src/main/java/com/mechuragi/mechuragi_server/auth/service/EmailService.java:38`
  - 변경 전: `@Transactional`
  - 변경 후: `@Transactional(propagation = Propagation.REQUIRES_NEW)`

**참고:**
- 이메일 발송 실패 시에도 회원가입은 완료됨
- 사용자는 나중에 "이메일 인증 메일 재발송" API를 통해 다시 인증 메일을 받을 수 있음

---

## 참고사항
- JWT 토큰은 "Bearer " 접두사 필요
- Access Token 유효기간: 24시간
- Refresh Token 유효기간: 7일
- 비밀번호 규칙: 8~20자, 영문/숫자/특수문자 포함
- 닉네임 규칙: 2~20자, 한글/영문/숫자만 가능
