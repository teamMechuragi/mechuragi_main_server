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
# dev.env 파일 확인
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

---

## 테스트 일자: 2025-10-19

---

## 개선 작업 내역

### 작업 1: OAuth2 카카오 로그인 - 이메일만 받아오도록 수정
**작업 일시:** 2025-10-19

**변경 사항:**
카카오 OAuth2 로그인 시 이메일 정보만 받아오고, 닉네임과 프로필 이미지는 서버에서 자동 생성하도록 변경

**수정 파일:**

1. **OAuth2Attributes.java** (`src/main/java/com/mechuragi/mechuragi_server/auth/dto/OAuth2Attributes.java`)
   - `nickname`, `profileImageUrl` 필드 제거
   - `ofKakao()` 메서드에서 이메일만 받아오도록 수정
   - `toEntity()` 메서드에 `nickname` 파라미터 추가

2. **Member.java** (`src/main/java/com/mechuragi/mechuragi_server/domain/member/entity/Member.java:101-105`)
   - `appendIdToNickname()` 메서드 추가
   - 랜덤 닉네임 + 멤버 ID(Long → int 변환)를 조합하여 최종 닉네임 생성

3. **CustomOAuth2UserService.java** (`src/main/java/com/mechuragi/mechuragi_server/auth/service/CustomOAuth2UserService.java`)
   - `NicknameGenerator` 의존성 주입 추가
   - `saveOrUpdate()` 메서드 수정:
     - 신규 회원: 랜덤 닉네임 생성 → 임시 저장 → ID 발급 → 닉네임+ID 조합
     - 기존 회원: 프로필 업데이트 로직 제거 (조회만 수행)

**결과:**
- 카카오에서 이메일만 받아옴
- 닉네임: `NicknameGenerator`로 생성된 랜덤값 + 멤버 ID (예: "행복한곰1")
- 프로필 이미지: null

---

### 작업 2: 닉네임 자동생성 API 추가 (일반 로그인용)
**작업 일시:** 2025-10-19

**변경 사항:**
일반 회원가입 시 사용할 수 있는 랜덤 닉네임 생성 API 추가

**생성/수정 파일:**

1. **NicknameResponse.java** (신규 생성)
   - 경로: `src/main/java/com/mechuragi/mechuragi_server/auth/dto/NicknameResponse.java`
   - 닉네임을 반환하는 응답 DTO

2. **AuthController.java** (`src/main/java/com/mechuragi/mechuragi_server/auth/controller/AuthController.java:88-93`)
   - `NicknameGenerator` 의존성 주입 추가
   - **GET `/api/auth/nickname/generate`** API 추가

**API 사용 예시:**
```bash
GET /api/auth/nickname/generate

# 응답
{
  "nickname": "행복한곰"
}
```

**사용 시나리오:**
프론트엔드에서 회원가입 화면에 랜덤 닉네임을 제안하고, 사용자가 원하면 그대로 사용하거나 수정 가능

---

### 작업 3: 이메일 인증 로직 개선 - 회원가입 전 인증으로 변경
**작업 일시:** 2025-10-19

**변경 사항:**
기존: 회원가입 → 이메일 인증
개선: 이메일 인증 → 회원가입

**수정 파일:**

1. **EmailService.java** (`src/main/java/com/mechuragi/mechuragi_server/auth/service/EmailService.java`)
   - **sendVerificationEmail()** (38-72줄):
     - 회원 조회 로직 제거
     - 이메일 중복 체크 추가
     - `memberId` 대신 `email`로 인증 정보 관리
   - **verifyEmail()** (78-98줄):
     - 회원 조회 로직 제거
     - `email`로 인증 정보 조회
     - 회원 이메일 인증 처리 제거 (회원가입 전이므로)

2. **EmailVerification.java** (`src/main/java/com/mechuragi/mechuragi_server/auth/entity/EmailVerification.java:26-27`)
   - `memberId` 필드를 `email` 필드로 변경
   - 회원가입 전에 이메일만으로 인증 관리 가능

3. **EmailVerificationRepository.java** (`src/main/java/com/mechuragi/mechuragi_server/auth/repository/EmailVerificationRepository.java`)
   - `findByMemberId()` → `findByEmail()` 변경
   - `findByMemberIdAndVerificationCode()` → `findByEmailAndVerificationCode()` 변경
   - `deleteByMemberId()` → `deleteByEmail()` 변경

4. **AuthService.java** (`src/main/java/com/mechuragi/mechuragi_server/auth/service/AuthService.java:36-80`)
   - **signup()** 메서드 수정:
     - 이메일 인증 여부 확인 로직 추가 (43-52줄)
     - 임시 닉네임으로 Member 저장 (69줄)
     - `appendIdToNickname()`로 최종 닉네임 생성 (72줄)
     - 회원가입 완료 후 이메일 인증 정보 삭제 (75줄)

**개선된 회원가입 플로우:**
```
1. 이메일 인증 메일 발송: POST /api/auth/email/send
   - 이메일 중복 확인 → 인증 코드 생성 → 이메일 발송

2. 이메일 인증 코드 확인: POST /api/auth/email/verify
   - 인증 코드 확인 → 인증 완료 처리

3. 회원가입: POST /api/auth/signup
   - 이메일 인증 완료 여부 확인
   - 임시 닉네임으로 저장 → 닉네임 + ID 조합 (예: "행복한곰1")
   - 이메일 인증 정보 삭제
```

**장점:**
- 이메일 중복을 회원가입 전에 확인 가능
- 인증되지 않은 이메일로 회원가입 불가
- 트랜잭션 경계가 명확해짐
- 이메일 인증 정보가 회원 테이블과 독립적으로 관리됨

**주의사항:**
- 이메일 인증은 30분 유효
- 회원가입 시 반드시 이메일 인증이 완료되어야 함
- 닉네임은 자동으로 "랜덤닉네임 + 멤버ID" 형태로 생성됨

---

### 작업 4: 닉네임 생성 규칙 통일
**작업 일시:** 2025-10-19

**변경 사항:**
일반 회원가입과 OAuth2 로그인 모두 동일한 닉네임 생성 규칙 적용

**닉네임 생성 규칙:**
- 형식: `{랜덤닉네임}{멤버ID}`
- 예시: "행복한곰1", "맛있는피자123"
- 랜덤닉네임: `NicknameGenerator`에서 형용사 + 명사 조합으로 생성
- 멤버ID: Long 타입의 기본키를 int로 변환하여 사용

**적용 위치:**
1. **일반 회원가입** (`AuthService.signup()`):
   - 프론트엔드에서 받은 닉네임 + 멤버ID 조합
   - 사용자가 API로 받은 랜덤 닉네임을 그대로 사용하거나 수정 가능

2. **OAuth2 로그인** (`CustomOAuth2UserService.saveOrUpdate()`):
   - 서버에서 자동으로 랜덤 닉네임 생성 + 멤버ID 조합
   - 카카오에서 받은 닉네임 사용 안 함

**참고:**
- `NicknameGenerator`는 형용사 20개, 명사 20개를 조합하여 총 400가지 닉네임 생성 가능
- 멤버ID를 붙여서 유니크함을 보장
- 닉네임은 가입 후 프로필 수정 API로 변경 가능

---

## 테스트 일자: 2025-10-27

---

## 배포 관련 트러블슈팅

### 문제 4: Swagger UI 접속 불가 - EC2 배포 후 외부 접속 실패
**발생 일시:** 2025-10-27

**증상:**
```
EC2 인스턴스 실행 후 http://15.165.136.100:8080/swagger-ui/index.html 접속 불가
```

**원인 분석:**
Swagger 설정은 정상이지만, 인프라 설정 문제로 외부에서 접근할 수 없는 경우

**가능한 원인 및 해결 방법:**

#### 1. EC2 Security Group 설정 (가장 가능성 높음)
**원인:** 8080 포트가 외부에서 접근 가능하도록 열려있지 않음

**확인 방법:**
- AWS Console → EC2 → 해당 인스턴스 선택 → Security 탭 → Security Groups 클릭
- Inbound rules에 8080 포트가 있는지 확인

**해결 방법:**
```
Type: Custom TCP
Port: 8080
Source: 0.0.0.0/0 (모든 IP 허용)
또는
Source: [특정 IP 범위] (보안을 위해 특정 IP만 허용)
```

#### 2. Docker 컨테이너 상태 확인
**원인:** 컨테이너가 실제로 실행되지 않거나 중단됨

**확인 방법:**
```bash
# EC2 인스턴스에 SSH 접속
ssh ubuntu@15.165.136.100

# 컨테이너 실행 상태 확인
sudo docker ps | grep mechuragi-main-server

# 만약 컨테이너가 없다면 중단된 컨테이너 확인
sudo docker ps -a | grep mechuragi-main-server
```

**해결 방법:**
```bash
# 컨테이너 로그 확인
sudo docker logs mechuragi-main-server

# 최근 로그만 보기
sudo docker logs --tail 100 mechuragi-main-server

# 컨테이너가 중단되었다면 재시작
sudo docker start mechuragi-main-server

# 재시작해도 안 되면 새로 실행
sudo docker run -d -p 8080:8080 --name mechuragi-main-server [이미지명]
```

#### 3. 애플리케이션 Health Check
**원인:** 컨테이너는 실행 중이지만 애플리케이션이 정상적으로 시작되지 않음

**확인 방법:**
```bash
# EC2 내부에서 테스트
curl http://localhost:8080/actuator/health

# Swagger 경로 직접 확인
curl http://localhost:8080/swagger-ui/index.html

# 응답이 없거나 에러가 나면 로그 확인
sudo docker logs mechuragi-main-server 2>&1 | grep -i error
```

**주요 확인 사항:**
- DB 연결 실패 여부
- Redis 연결 실패 여부
- 환경 변수 누락 여부
- 포트 바인딩 충돌 여부

#### 4. 배포 상태 확인
**원인:** GitHub Actions 배포가 실패했거나 완료되지 않음

**확인 방법:**
- GitHub Repository → Actions 탭
- 최근 deploy workflow 상태 확인
- 실패한 단계가 있는지 확인

**해결 방법:**
- 배포 실패 시: 에러 로그 확인 후 수정하고 재배포
- 배포 대기 중: 완료될 때까지 대기

#### 5. 애플리케이션 설정 확인
**현재 설정 (정상):**
- `application-dev.yml:2-3` - `port: 8080`, `address: 0.0.0.0` (모든 인터페이스 허용)
- `.github/workflows/deploy.yml:101` - `-p 8080:8080` (포트 매핑 정상)

**문제 해결 체크리스트:**
```
✅ 1. EC2 Security Group에 8080 포트 Inbound rule 추가
✅ 2. Docker 컨테이너 실행 상태 확인
✅ 3. EC2 내부에서 localhost:8080 접속 확인
✅ 4. 컨테이너 로그에서 에러 확인
✅ 5. GitHub Actions 배포 상태 확인
```

**가장 먼저 확인해야 할 것:**
👉 **EC2 Security Group의 8080 포트 Inbound rule 설정**

**참고:**
- Swagger UI 경로: `/swagger-ui/index.html`
- Health Check 경로: `/actuator/health`
- 애플리케이션 로그 실시간 확인: `sudo docker logs -f mechuragi-main-server`

---

## 테스트 일자: 2025-10-28

---

## 배포 및 인증 관련 트러블슈팅

### 문제 5: AWS SES Production 승인 대기 - 이메일 인증 로직 임시 비활성화
**발생 일시:** 2025-10-28

**증상:**
AWS SES production 요청이 거부되어 이메일 발송이 불가능한 상태

**문제 상황:**
- SES가 Sandbox 모드에서만 작동
- Production 모드 승인 대기 중
- 이메일 인증이 필수인 회원가입 플로우가 작동하지 않음

**해결 방법:**
회원가입 시 이메일 인증 로직을 임시로 주석처리하여 AWS 승인 전까지 회원가입 가능하도록 조치

**수정 파일:**
`src/main/java/com/mechuragi/mechuragi_server/auth/service/AuthService.java:36-80`

**수정 내용:**

1. **이메일 인증 확인 로직 주석처리** (42-55줄)
```java
// TODO: AWS SES production 승인 대기 중 - 이메일 인증 로직 임시 비활성화
// 이메일 인증 여부 확인
/*
EmailVerification emailVerification = emailVerificationRepository.findByEmail(request.getEmail())
        .orElseThrow(() -> new IllegalArgumentException("이메일 인증이 필요합니다."));

if (!emailVerification.getVerified()) {
    throw new IllegalArgumentException("이메일 인증이 완료되지 않았습니다.");
}

if (emailVerification.isExpired()) {
    throw new IllegalArgumentException("이메일 인증이 만료되었습니다. 다시 인증해주세요.");
}
*/
```

2. **이메일 인증 정보 삭제 로직 주석처리** (77-79줄)
```java
// TODO: AWS SES production 승인 대기 중 - 이메일 인증 정보 삭제 로직 임시 비활성화
// 이메일 인증 정보 삭제 (회원가입 완료 후)
// emailVerificationRepository.delete(emailVerification);
```

**결과:**
- ✅ 이메일 인증 없이 회원가입 가능
- ✅ 빌드 성공 (`./gradlew clean build -x test`)
- ✅ 이메일 인증 API는 유지되어 향후 쉽게 복구 가능

**복구 방법:**
AWS SES production 승인 후 주석을 제거하면 원래 인증 플로우로 복구 가능

**주의사항:**
- 이메일 인증 없이 회원가입이 가능하므로 이메일 중복 체크만으로 검증
- Production 승인 후 반드시 주석 해제 필요

---

### 문제 6: GitHub Actions에서 dev 브랜치 배포 안되는 문제
**발생 일시:** 2025-10-28

**증상:**
```
dev 브랜치에 머지 시:
✅ test job 실행
❌ deploy job 스킵됨
```

**원인 분석:**
`.github/workflows/deploy.yml:39`의 deploy job 조건이 main 브랜치만 허용
```yaml
if: github.ref == 'refs/heads/main' && github.event_name == 'push'
```

**해결 방법:**
deploy job의 조건을 dev 브랜치도 포함하도록 수정

**수정 파일:**
`.github/workflows/deploy.yml:39`

**수정 내용:**
```yaml
# 변경 전
if: github.ref == 'refs/heads/main' && github.event_name == 'push'

# 변경 후
if: (github.ref == 'refs/heads/main' || github.ref == 'refs/heads/dev') && github.event_name == 'push'
```

**결과:**
- ✅ dev 브랜치 푸시 시: test + deploy 모두 실행
- ✅ main 브랜치 푸시 시: test + deploy 모두 실행

**참고:**
- 현재 `SPRING_PROFILES_ACTIVE=dev`로 하드코딩되어 있음
- 필요시 브랜치에 따라 프로파일 분기 가능

---

### 문제 7: Docker 컨테이너 배포 시 포트 충돌 문제
**발생 일시:** 2025-10-28

**에러 메시지:**
```
docker: Error response from daemon: failed to set up container networking:
driver failed programming external connectivity on endpoint mechuragi-main-server:
Bind for 0.0.0.0:8080 failed: port is already allocated
```

**원인 분석:**
1. 기존 컨테이너 이름: `mechuragi-server:dev`
2. 새 컨테이너 이름: `mechuragi-main-server`
3. 배포 스크립트는 `mechuragi-main-server`만 찾아서 삭제 시도
4. 기존 컨테이너가 삭제되지 않아 8080 포트 충돌 발생

**해결 방법:**
8080 포트를 사용하는 모든 컨테이너를 자동으로 찾아서 정리하도록 배포 스크립트 개선

**수정 파일:**
`.github/workflows/deploy.yml:95-110`

**수정 내용:**

**변경 전:**
```bash
# Stop and remove existing container (ignore errors if not exists)
sudo docker stop mechuragi-main-server 2>/dev/null || echo "Container not running, skipping stop"
sudo docker rm mechuragi-main-server 2>/dev/null || echo "Container not found, skipping removal"
```

**변경 후:**
```bash
# Stop and remove all containers using port 8080
echo "Checking for containers using port 8080..."
CONTAINER_IDS=$(sudo docker ps -q --filter "publish=8080")
if [ -n "$CONTAINER_IDS" ]; then
  echo "Stopping containers using port 8080: $CONTAINER_IDS"
  sudo docker stop $CONTAINER_IDS
  sudo docker rm $CONTAINER_IDS
else
  echo "No containers using port 8080 found"
fi

# Also clean up by name (legacy support)
sudo docker stop mechuragi-main-server 2>/dev/null || true
sudo docker rm mechuragi-main-server 2>/dev/null || true
sudo docker stop mechuragi-server 2>/dev/null || true
sudo docker rm mechuragi-server 2>/dev/null || true
```

**개선 효과:**
1. ✅ 포트 기반 자동 감지: 8080 포트 사용 컨테이너 자동 정리
2. ✅ 이름 기반 백업: 여러 컨테이너 이름 패턴 대응
3. ✅ 에러 무시: 컨테이너가 없어도 배포 실패 안 함

**결과:**
- 기존 `mechuragi-server:dev` 컨테이너 자동 삭제
- 새 `mechuragi-main-server` 컨테이너 정상 실행
- 포트 충돌 없이 배포 성공

**교훈:**
- 컨테이너 이름이 변경될 수 있으므로 포트 기반으로 찾는 것이 더 안전
- 여러 이름 패턴을 함께 처리하여 호환성 확보
- 에러 처리를 통해 배포 중단 방지

---

## 테스트 일자: 2025-11-12

---

## 알림 관련 트러블슈팅

### 문제 8: 투표 종료 알림이 DB에 저장되지 않는 문제
**발생 일시:** 2025-11-12

**증상:**
```
- 투표 종료 10분 전 알림: DB 저장 ✅ 정상
- 투표 종료 알림: Redis Pub/Sub 발송 ✅ 정상, DB 저장 ❌ 실패
```

**에러 로그:**
```
DEBUG c.m.m.d.n.event.VoteEventListener : 투표 종료 이벤트 수신: voteId=22, authorId=6
DEBUG c.m.m.d.n.event.VoteEventListener : 투표 종료 알림 DB 저장 호출: voteId=22, authorId=6
DEBUG c.m.m.d.n.service.NotificationService : 알림 저장 시도: memberId=6, voteId=22, type=COMPLETED
DEBUG c.m.m.d.n.service.NotificationService : 알림 저장 완료: notificationId=null, memberId=6, voteId=22, type=COMPLETED
DEBUG c.m.m.d.n.event.VoteEventListener : 투표 종료 알림 DB 저장 결과: notificationId=null
```

**원인 분석:**

**핵심 문제:** `notificationId=null` - `notificationRepository.save()`가 제대로 커밋되지 않음

**상세 분석:**

1. **트랜잭션 전파 문제**
   ```java
   @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
   public void handleVoteCompleted(VoteCompletedEvent event) {
       // 이 시점에는 이미 트랜잭션이 커밋된 후
       notificationService.createNotification(...); // 새 트랜잭션 필요
   }
   ```

2. **VoteEventListener 실행 시점**
   - `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`
   - 투표 종료 처리 트랜잭션이 **커밋된 이후**에 실행됨
   - 이 시점에는 활성 트랜잭션이 없음

3. **NotificationService의 @Transactional 문제**
   ```java
   @Transactional  // 기본 전파 옵션: REQUIRED
   public Notification createNotification(...) {
       // REQUIRED: 기존 트랜잭션 사용, 없으면 새로 생성
       // 하지만 AFTER_COMMIT 시점에는 트랜잭션 컨텍스트가 없어서
       // 제대로 커밋되지 않음
   }
   ```

4. **투표 10분 전 알림은 왜 정상?**
   ```java
   @Transactional  // VotePostService.notifyVoteEndingSoon()
   public void notifyVoteEndingSoon(Long voteId, String title) {
       // 이 메서드 자체가 @Transactional 내에서 실행
       notificationService.createNotification(...);
       // 같은 트랜잭션 내에서 실행되어 정상 저장됨
   }
   ```

**해결 방법:**

`NotificationService.createNotification()`에 **새 트랜잭션 시작** 전파 옵션 추가

**수정 파일:**
`src/main/java/com/mechuragi/mechuragi_server/domain/notification/service/NotificationService.java:30`

**수정 내용:**
```java
// 변경 전
@Transactional
public Notification createNotification(Long memberId, Long voteId, String title, VoteNotificationType type) {

// 변경 후
@Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
public Notification createNotification(Long memberId, Long voteId, String title, VoteNotificationType type) {
```

**트랜잭션 전파 옵션 설명:**
- `REQUIRED` (기본값): 기존 트랜잭션 사용, 없으면 새로 생성
- `REQUIRES_NEW`: **항상 새로운 독립적인 트랜잭션 시작**
  - 기존 트랜잭션이 있어도 새로 생성
  - 기존 트랜잭션과 독립적으로 커밋/롤백
  - `@TransactionalEventListener(AFTER_COMMIT)` 사용 시 필수

**결과:**
- ✅ 투표 종료 알림 DB 저장 정상 동작
- ✅ notificationId 정상 생성
- ✅ 투표 10분 전 알림도 계속 정상 동작 (기존 트랜잭션과 독립적으로 실행)

**디버깅 로그 추가:**

추가로 디버깅을 위해 `log.debug()` 로그를 추가했습니다.

**수정 파일:**
1. `NotificationService.java:32, 57-58`
2. `VoteEventListener.java:30, 43-44, 54, 61`

**추가된 로그:**
```java
// NotificationService
log.debug("알림 저장 시도: memberId={}, voteId={}, type={}", ...);
log.debug("알림 저장 완료: notificationId={}, memberId={}, voteId={}, type={}", ...);

// VoteEventListener
log.debug("투표 종료 이벤트 수신: voteId={}, authorId={}", ...);
log.debug("투표 종료 알림 처리 시작: voteId={}, authorId={}, voteNotificationEnabled={}", ...);
log.debug("투표 종료 알림 DB 저장 호출: voteId={}, authorId={}", ...);
log.debug("투표 종료 알림 DB 저장 결과: notificationId={}", ...);
```

**디버그 로그 vs 일반 로그:**
- `log.debug()`: 개발/디버깅용 상세 로그
- `log.info()`: 운영 환경 주요 정보 로그
- `log.warn()`: 경고 로그
- `log.error()`: 에러 로그

**로그 레벨 설정 (application.yml):**
```yaml
logging:
  level:
    com.mechuragi: DEBUG  # 개발 환경 - debug 로그 출력
    # com.mechuragi: INFO  # 운영 환경 - debug 로그 숨김
```

**교훈:**
- `@TransactionalEventListener(AFTER_COMMIT)` 사용 시 새 트랜잭션이 필요한 작업은 `REQUIRES_NEW` 전파 옵션 필수
- 같은 서비스 메서드라도 호출 시점(트랜잭션 내부 vs 외부)에 따라 동작이 달라질 수 있음
- 디버그 로그를 적극 활용하여 트랜잭션 경계와 실행 흐름 파악 필요
- `notificationId=null`은 트랜잭션 미커밋의 명확한 신호

**관련 파일:**
- `VoteEventListener.java` - 투표 종료 이벤트 리스너
- `NotificationService.java` - 알림 저장 서비스
- `VotePostService.java` - 투표 10분 전 알림 (정상 동작)

---
