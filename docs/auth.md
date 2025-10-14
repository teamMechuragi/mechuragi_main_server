# JWT 인증 + Spring Security 회원가입/로그인 구현 계획

## 전체 구현 계획

### 1단계: 프로젝트 기반 설정
- **build.gradle 의존성 추가**
  - Spring Security
  - JWT (jjwt 라이브러리)
  - AWS SES (이메일 인증)
  - OAuth2 Client (카카오 소셜 로그인)
  - BCrypt (비밀번호 암호화)
- **application.yml 설정**
  - JWT 설정 (secret, expiration)
  - AWS SES 설정
  - OAuth2 카카오 클라이언트 설정

### 2단계: member 패키지 구현 (회원 도메인)
- **Member 엔티티**
  - 기본 정보: id, email, nickname, password
  - 인증 정보: emailVerified (이메일 인증 여부), provider (일반/카카오)
  - 역할: role (USER, ADMIN)
  - Audit 필드: createdAt, updatedAt
- **MemberRepository**
  - findByEmail, existsByEmail, existsByNickname 등
- **MemberService/Impl**
  - 회원 조회, 수정, 삭제 등 회원 관리 기능
- **MemberController**
  - 회원 정보 조회/수정 API

### 3단계: auth 패키지 - JWT 토큰 처리
- **JWT 유틸리티 클래스**
  - 토큰 생성 (generateToken)
  - 토큰 검증 (validateToken)
  - 토큰에서 정보 추출 (getEmailFromToken)
- **RefreshToken 엔티티** (선택사항)
  - Access Token + Refresh Token 구조 사용 시

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
