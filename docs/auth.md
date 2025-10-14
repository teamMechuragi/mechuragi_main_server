# JWT 인증 + Spring Security 회원가입/로그인 구현 계획

## 전체 구현 계획

### 1단계: 프로젝트 기반 설정 ✅ 완료
- **build.gradle 의존성 추가**
  - ✅ Spring Security (주석 해제)
  - ✅ Spring OAuth2 Client (주석 해제)
  - ✅ JWT (io.jsonwebtoken:jjwt-api:0.12.3, jjwt-impl, jjwt-jackson)
  - ✅ AWS SES (software.amazon.awssdk:ses:2.20.26)
  - ✅ BCrypt (Spring Security에 포함)

- **application.yml 설정**
  - ✅ JWT 설정 (secret: ${JWT_SECRET}, expiration: 86400000)
  - ✅ AWS SES 설정 (from-email, region)
  - ✅ OAuth2 카카오 클라이언트 설정 (client-id, client-secret, redirect-uri, scope 등)

- **.env 환경 변수**
  - ✅ JWT_SECRET
  - ✅ SES_FROM_EMAIL, SES_DOMAIN, SUPPORT_EMAIL, SEND_TEST_EMAIL, TEST_EMAIL
  - ✅ KAKAO_CLIENT_ID, KAKAO_CLIENT_SECRET

### 2단계: member 도메인 구현 ✅ 완료
- **Member 엔티티**
  - ✅ 기본 정보: id, email (unique), nickname (unique), password, profileImageUrl
  - ✅ 인증 정보: emailVerified (이메일 인증 여부), provider (NORMAL/KAKAO)
  - ✅ 역할: role (USER, ADMIN)
  - ✅ 상태: status (ACTIVE, INACTIVE, SUSPENDED)
  - ✅ Audit 필드: createdAt, updatedAt
  - ✅ 도메인 메서드: updateProfile, updatePassword, verifyEmail, changeStatus
  - ✅ **DB 제약조건**: email, nickname에 unique 제약조건 (동시성 안전)

- **Enum 분리** (entity/type 패키지)
  - ✅ AuthProvider (NORMAL, KAKAO)
  - ✅ Role (USER, ADMIN)
  - ✅ MemberStatus (ACTIVE, INACTIVE, SUSPENDED)

- **MemberRepository**
  - ✅ findByEmail, existsByEmail, existsByNickname

- **MemberService**
  - ✅ 회원 조회 (ID, 이메일)
  - ✅ 회원 정보 수정 (닉네임 중복 체크 포함)
  - ✅ 비밀번호 변경 (현재 비밀번호 확인 포함)
  - ✅ 회원 탈퇴 (소프트 삭제)
  - ✅ 이메일/닉네임 중복 확인

- **MemberController**
  - ✅ GET /api/members/{memberId} - 회원 조회
  - ✅ PUT /api/members/{memberId} - 회원 정보 수정
  - ✅ PUT /api/members/{memberId}/password - 비밀번호 변경
  - ✅ DELETE /api/members/{memberId} - 회원 탈퇴
  - ✅ GET /api/members/check/email - 이메일 중복 체크
  - ✅ GET /api/members/check/nickname - 닉네임 중복 체크

- **DTO**
  - ✅ MemberResponse - 회원 정보 응답
  - ✅ UpdateMemberRequest - 회원 정보 수정 요청
  - ✅ UpdatePasswordRequest - 비밀번호 변경 요청

- **중복 검증 전략** (이중 방어)
  - ✅ 애플리케이션 레벨: existsByEmail/Nickname (빠른 피드백)
  - ✅ 데이터베이스 레벨: unique 제약조건 (동시성 안전, 최종 방어선)

### 3단계: auth 패키지 - JWT 토큰 처리 ✅ 완료
- **auth 패키지 구조**
  - ✅ config/ - SecurityConfig, JwtConfig (4단계 예정)
  - ✅ entity/ - RefreshToken
  - ✅ dto/ - 인증 관련 DTO (5단계 예정)
  - ✅ service/ - AuthService, JwtService (5단계 예정)
  - ✅ controller/ - AuthController (5단계 예정)
  - ✅ filter/ - JwtAuthenticationFilter (4단계 예정)
  - ✅ util/ - JwtTokenProvider
  - ✅ repository/ - RefreshTokenRepository

- **JwtTokenProvider (JWT 유틸리티 클래스)**
  - ✅ generateAccessToken() - Access Token 생성 (24시간)
  - ✅ generateRefreshToken() - Refresh Token 생성 (7일)
  - ✅ validateToken() - 토큰 유효성 검증
  - ✅ getEmailFromToken() - 토큰에서 이메일 추출
  - ✅ getMemberIdFromToken() - 토큰에서 회원 ID 추출
  - ✅ getRoleFromToken() - 토큰에서 역할 추출
  - ✅ getExpirationFromToken() - 토큰 만료 시간 조회

- **RefreshToken 엔티티**
  - ✅ memberId (unique) - 회원 ID
  - ✅ token (unique) - Refresh Token 값
  - ✅ expiryDate - 만료 시간
  - ✅ createdAt - 생성 시간
  - ✅ updateToken() - 토큰 갱신
  - ✅ isExpired() - 만료 여부 확인

- **RefreshTokenRepository**
  - ✅ findByMemberId() - 회원 ID로 토큰 조회
  - ✅ findByToken() - 토큰으로 조회
  - ✅ deleteByMemberId() - 회원 ID로 토큰 삭제 (로그아웃)

### 4단계: auth 패키지 - Spring Security 설정
- **SecurityConfig**
  - SecurityFilterChain 설정
  - JWT 필터 등록
  - 인증/인가 경로 설정 (permitAll, authenticated)
  - CORS 설정
- **JwtAuthenticationFilter**
  - 요청에서 JWT 추출 및 검증
  - SecurityContext에 인증 정보 설정
- **CustomUserDetailsService**
  - loadUserByUsername 구현 (이메일로 회원 조회)

### 5단계: auth 패키지 - 일반 회원가입/로그인
- **회원가입 (Signup)**
  - DTO: SignupRequest (email, password, nickname)
  - 비밀번호 암호화 (BCryptPasswordEncoder)
  - 이메일 중복 체크, 닉네임 중복 체크
  - 회원 저장 + 이메일 인증 메일 발송
- **로그인 (Login)**
  - DTO: LoginRequest (email, password)
  - 비밀번호 검증
  - JWT 토큰 발급 (Access Token, Refresh Token)
  - DTO: LoginResponse (tokens, memberInfo)
- **AuthController**
  - POST /api/auth/signup
  - POST /api/auth/login
  - POST /api/auth/logout
  - POST /api/auth/refresh (토큰 재발급)

### 6단계: 이메일 인증 (AWS SES)
- **EmailVerification 엔티티**
  - memberId, verificationCode, expiresAt, verified
- **EmailService**
  - AWS SES 연동
  - 인증 메일 발송 (sendVerificationEmail)
  - 인증 코드 생성
- **이메일 인증 API**
  - POST /api/auth/email/send (인증 메일 발송/재발송)
  - POST /api/auth/email/verify (인증 코드 확인)
  - Member.emailVerified 업데이트

### 7단계: 소셜 로그인 (카카오)
- **OAuth2 설정**
  - application.yml에 카카오 클라이언트 설정
  - OAuth2UserService 커스터마이징
- **카카오 로그인 처리**
  - OAuth2SuccessHandler (로그인 성공 시 JWT 발급)
  - 카카오에서 받은 정보로 회원 자동 가입 or 로그인
  - provider = "KAKAO" 설정
- **카카오 로그인 API**
  - GET /api/auth/oauth2/authorization/kakao (카카오 로그인 페이지로 리다이렉트)
  - GET /api/auth/login/oauth2/code/kakao (콜백 처리)

### 8단계: 공통 예외 처리 및 응답 형식
- **GlobalExceptionHandler**
  - 인증 실패, 권한 없음, 중복 데이터 등 처리
- **공통 응답 DTO**
  - ApiResponse<T> (success, message, data)
  - ErrorResponse (error, message, details)

## 패키지 구조

```
auth/
├── config/          # SecurityConfig, JwtConfig
├── entity/          # RefreshToken, EmailVerification
├── dto/             # SignupRequest, LoginRequest/Response
├── service/         # AuthService, EmailService, JwtService
├── controller/      # AuthController
└── filter/          # JwtAuthenticationFilter

member/
├── entity/          # Member (회원 엔티티)
├── dto/             # MemberRequest, MemberResponse
├── repository/      # MemberRepository
├── service/         # MemberService
└── controller/      # MemberController
```

## 구현 정보

### 회원 정보
- 이메일 (필수, 인증 필요)
- 비밀번호 (일반 로그인만)
- 닉네임 (필수)
- 이메일 인증 여부
- 가입 유형 (일반/카카오)

### 로그인 방식
- 일반 로그인: 이메일 + 비밀번호
- 소셜 로그인: 카카오 OAuth2

### 이메일 인증
- AWS SES 사용
- 인증 코드 발송 및 검증
