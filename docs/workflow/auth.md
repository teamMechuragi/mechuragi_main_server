# JWT 인증 + Spring Security 회원가입/로그인 구현 계획

##  JWT 인증 동작 흐름 (@AuthenticationPrincipal)

```
1. 클라이언트 요청
   └─ Authorization: Bearer {JWT_TOKEN}

2. JwtAuthenticationFilter 실행
   └─ doFilterInternal()
      ├─ JWT 토큰 추출 (Authorization 헤더에서 "Bearer " 제거)
      ├─ jwtTokenProvider.validateToken() - 토큰 유효성 검증
      └─ 검증 성공 시:
         ├─ jwtTokenProvider.getEmailFromToken() - 이메일 추출
         ├─ customUserDetailsService.loadUserByUsername(email)
         │  └─ memberRepository.findByEmail(email)
         │     └─ Member 조회
         │        └─ new CustomUserDetails(member) 생성
         ├─ UsernamePasswordAuthenticationToken 생성
         │  └─ principal: CustomUserDetails
         │  └─ authorities: [ROLE_USER] or [ROLE_ADMIN]
         └─ SecurityContextHolder.getContext().setAuthentication(authentication)
            └─ SecurityContext에 인증 정보 저장

3. 컨트롤러 메서드 실행
   └─ @AuthenticationPrincipal CustomUserDetails userDetails
      └─ Spring Security ArgumentResolver가 SecurityContext에서 자동으로 가져옴
         ├─ userDetails.getMemberId() - 회원 ID
         ├─ userDetails.getEmail() - 이메일
         ├─ userDetails.getNickname() - 닉네임
         └─ userDetails.getAuthorities() - 권한 목록

4. 응답 반환
```
### 현재 프로젝트에 커스텀 필터 체인 보다 컨트롤러 API 방식이 적합한 이유

  이유:
  1. ✅ JWT 토큰 기반
  2. ✅ RESTful API 구조
  3. ✅ 프론트엔드 분리
  4. ✅ OAuth2와 통합
  5. ✅ JwtAuthenticationFilter 사용

## 전체 구현 흐름

### 1단계: 프로젝트 기반 설정 ✅ 완료
- **build.gradle 의존성 추가**
  - ✅ Spring Security
  - ✅ Spring OAuth2 Client
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

### 4단계: auth 패키지 - Spring Security 설정 ✅ 완료
- **CustomUserDetails (UserDetails 구현)**
  - ✅ Member 엔티티를 감싸는 인증 정보 클래스
  - ✅ getMemberId(), getEmail(), getNickname() - 편의 메서드
  - ✅ getAuthorities() - "ROLE_" + role 반환
  - ✅ isEnabled() - ACTIVE 상태만 true
  - ✅ **@AuthenticationPrincipal로 컨트롤러에서 직접 사용 가능**

- **CustomUserDetailsService**
  - ✅ loadUserByUsername() 구현 (이메일로 회원 조회)
  - ✅ Member → CustomUserDetails 변환

- **JwtAuthenticationFilter**
  - ✅ OncePerRequestFilter 상속
  - ✅ Authorization 헤더에서 JWT 추출 ("Bearer " 제거)
  - ✅ JWT 유효성 검증
  - ✅ 토큰에서 이메일 추출 → UserDetails 조회
  - ✅ UsernamePasswordAuthenticationToken 생성
  - ✅ SecurityContext에 Authentication 설정

- **SecurityConfig**
  - ✅ SecurityFilterChain 설정
  - ✅ JWT 필터 등록 (UsernamePasswordAuthenticationFilter 앞)
  - ✅ CSRF 비활성화 (JWT 사용)
  - ✅ 세션 정책: STATELESS
  - ✅ 인증/인가 경로 설정:
    - permitAll: /api/auth/**, /api/test/**, /api/members/check/**, /actuator/**
    - hasRole("ADMIN"): /api/admin/**
    - authenticated: 그 외 모든 요청
  - ✅ CORS 설정 (localhost:3000, localhost:8080)
  - ✅ PasswordEncoder Bean (BCryptPasswordEncoder)

- **MemberService 업데이트**
  - ✅ PasswordEncoder 주입
  - ✅ updatePassword() - 현재 비밀번호 확인 + 새 비밀번호 암호화

### 5단계: auth 패키지 - 일반 회원가입/로그인 ✅ 완료
- **DTO**
  - ✅ SignupRequest - 회원가입 요청 (email, password, nickname)
  - ✅ LoginRequest - 로그인 요청 (email, password)
  - ✅ LoginResponse - 로그인 응답 (tokens, memberInfo)
  - ✅ TokenResponse - 토큰 정보 (accessToken, refreshToken, tokenType, expiresIn)

- **JwtService**
  - ✅ issueTokens() - Access Token, Refresh Token 발급 및 DB 저장
  - ✅ refreshAccessToken() - Refresh Token으로 Access Token 재발급
  - ✅ logout() - Refresh Token 삭제

- **MemberService 업데이트** (회원가입 로직 책임 분리)
  - ✅ signup() - 일반 회원가입 핵심 로직 (auth → member 도메인으로 이동)
    - ✅ 이메일 중복 체크
    - ✅ 닉네임 중복 체크
    - ✅ 비밀번호 암호화 (BCryptPasswordEncoder)
    - ✅ 회원 저장 (provider: NORMAL, role: USER, status: ACTIVE)
    - ✅ Member 엔티티 반환

- **AuthService**
  - ✅ signup() - 인증 관련 후처리
    - ✅ MemberService.signup() 호출 (회원 생성)
    - ✅ 이메일 인증 메일 발송 (EmailService 사용)
    - ✅ 발송 실패해도 회원가입은 완료 (재발송 가능)
  - ✅ login() - 일반 로그인
    - ✅ 이메일로 회원 조회
    - ✅ 소셜 로그인 계정 체크
    - ✅ 비밀번호 검증
    - ✅ 계정 상태 확인 (ACTIVE만 로그인 허용)
    - ✅ JWT 토큰 발급 (JwtService 사용)
  - ✅ logout() - 로그아웃 (Refresh Token 삭제)
  - ✅ refresh() - Access Token 재발급

- **AuthController**
  - ✅ POST /api/auth/signup - 회원가입
  - ✅ POST /api/auth/login - 로그인
  - ✅ POST /api/auth/logout - 로그아웃 (@AuthenticationPrincipal 사용)
  - ✅ POST /api/auth/refresh - Access Token 재발급 (Refresh-Token 헤더)

  - signup, login: 토큰 없음 → @AuthenticationPrincipal 사용 불가
  - logout: 토큰 있음 → @AuthenticationPrincipal 사용 가능
  - refresh: 토큰 만료 → Refresh Token만 사용

### 6단계: 이메일 인증 (AWS SES) ✅ 완료
- **EmailVerification 엔티티**
  - ✅ memberId (unique) - 회원 ID
  - ✅ verificationCode - 6자리 인증 코드
  - ✅ expiresAt - 만료 시간 (30분)
  - ✅ verified - 인증 완료 여부
  - ✅ createdAt - 생성 시간
  - ✅ isExpired() - 만료 여부 확인
  - ✅ verify() - 인증 완료 처리
  - ✅ updateCode() - 인증 코드 재발급

- **EmailVerificationRepository**
  - ✅ findByMemberId() - 회원 ID로 조회
  - ✅ findByMemberIdAndVerificationCode() - 인증 코드 검증용
  - ✅ deleteByMemberId() - 회원 ID로 삭제

- **DTO**
  - ✅ SendVerificationEmailRequest - 이메일 인증 메일 발송 요청
  - ✅ VerifyEmailRequest - 이메일 인증 코드 확인 요청

- **EmailService**
  - ✅ sendVerificationEmail() - 이메일 인증 메일 발송
    - ✅ 이미 인증된 이메일 체크
    - ✅ 6자리 랜덤 코드 생성
    - ✅ 인증 정보 DB 저장/업데이트
    - ✅ AWS SES를 통한 이메일 발송
    - ✅ HTML 템플릿 사용 (30분 유효 안내)
  - ✅ verifyEmail() - 이메일 인증 코드 확인
    - ✅ 인증 코드 일치 여부 확인
    - ✅ 만료 여부 확인
    - ✅ 인증 완료 처리
    - ✅ Member.emailVerified 업데이트
  - ✅ generateVerificationCode() - 6자리 랜덤 코드 생성 (100000~999999)
  - ✅ sendEmail() - AWS SES 이메일 발송 (HTML + Text)
  - ✅ buildEmailHtml() - 이메일 HTML 템플릿

- **AuthController**
  - ✅ POST /api/auth/email/send - 이메일 인증 메일 발송/재발송
  - ✅ POST /api/auth/email/verify - 이메일 인증 코드 확인

- **AuthService 업데이트**
  - ✅ signup() - 회원가입 시 이메일 인증 메일 자동 발송
    - ✅ 발송 실패해도 회원가입은 완료 (재발송 가능)

### 7단계: 소셜 로그인 (카카오) ✅ 완료
- **OAuth2Attributes DTO**
  - ✅ OAuth2 사용자 정보를 담는 DTO
  - ✅ of() - Provider에 따라 attributes 변환
  - ✅ ofKakao() - 카카오 로그인 정보 변환 (email, nickname, profileImageUrl)
  - ✅ toEntity() - Member 엔티티로 변환 (emailVerified: true)

- **CustomOAuth2UserService**
  - ✅ OAuth2UserService 구현
  - ✅ loadUser() - OAuth2 로그인 시 사용자 정보 처리
    - ✅ OAuth2User 정보 로드
    - ✅ registrationId로 Provider 확인 (kakao)
    - ✅ OAuth2Attributes로 변환
    - ✅ 회원 정보 조회 또는 생성
  - ✅ saveOrUpdate() - 회원 정보 저장/업데이트
    - ✅ 기존 회원: 프로필 정보 업데이트
    - ✅ 신규 회원: 회원 가입 (provider: KAKAO, role: USER, emailVerified: true)
    - ✅ 소셜 로그인 Provider 불일치 체크
  - ✅ generateUniqueNickname() - 중복되지 않는 닉네임 생성

- **OAuth2SuccessHandler**
  - ✅ SimpleUrlAuthenticationSuccessHandler 상속
  - ✅ onAuthenticationSuccess() - OAuth2 로그인 성공 시 처리
    - ✅ JWT 토큰 발급 (JwtService 사용)
    - ✅ 프론트엔드로 리다이렉트 (쿼리 파라미터로 토큰 전달)
    - ✅ redirectUri: ${oauth2.redirect-uri} (기본값: http://localhost:3000/oauth2/callback)

- **CustomUserDetails 업데이트**
  - ✅ OAuth2User 인터페이스 추가 구현
  - ✅ 일반 로그인용 생성자: CustomUserDetails(Member)
  - ✅ OAuth2 로그인용 생성자: CustomUserDetails(Member, attributes)
  - ✅ getAttributes() - OAuth2 사용자 정보 반환
  - ✅ getName() - 회원 ID 반환

- **SecurityConfig 업데이트**
  - ✅ CustomOAuth2UserService 주입
  - ✅ OAuth2SuccessHandler 주입
  - ✅ oauth2Login 설정 추가
    - ✅ userInfoEndpoint: customOAuth2UserService 사용
    - ✅ successHandler: oAuth2SuccessHandler 사용
  - ✅ permitAll 경로 추가: /oauth2/**, /login/oauth2/**

- **카카오 로그인 흐름**
  ```
  1. 프론트엔드 → 백엔드
     └─ GET /oauth2/authorization/kakao

  2. 백엔드 → 카카오
     └─ 카카오 로그인 페이지로 리다이렉트 (Spring Security 자동 처리)

  3. 카카오 → 백엔드 ⬅️ spring.security.oauth2.client.registration.kakao.redirect-uri 사용
     └─ GET /login/oauth2/code/kakao?code=...
        ├─ Spring Security가 카카오에서 access token 요청
        ├─ 카카오에서 사용자 정보 조회
        └─ CustomOAuth2UserService.loadUser() 호출
           ├─ OAuth2Attributes로 변환 (email, nickname, profileImageUrl)
           └─ saveOrUpdate() - 회원 자동 가입 or 정보 업데이트
              ├─ 신규 회원: Member 생성 (provider: KAKAO, emailVerified: true)
              └─ 기존 회원: 프로필 정보 업데이트

  4. 백엔드 → 프론트엔드 ⬅️ oauth2.redirect-uri 사용
     └─ OAuth2SuccessHandler.onAuthenticationSuccess() 호출
        ├─ JwtService.issueTokens() - JWT 토큰 발급
        └─ 프론트엔드로 리다이렉트
           GET http://localhost:3000/oauth2/callback
               ?accessToken=...
               &refreshToken=...
               &tokenType=Bearer
               &expiresIn=86400000
  ```

  **두 가지 redirect-uri 차이:**
  - `spring.security.oauth2.redirect-uri`: 카카오 → 백엔드 (OAuth2 콜백)
  - `oauth2.redirect-uri`: 백엔드 → 프론트엔드 (JWT 토큰 전달)

### 8단계: 회원가입 로직 책임 분리 ✅ 완료
- **관심사 분리**
  - ✅ 회원가입 핵심 로직: auth → member 도메인으로 이동
  - ✅ AuthService: 인증 관련 후처리(이메일 발송)만 담당
  - ✅ MemberService: 회원 생성 및 검증 로직 담당

- **이유**
  - ✅ 단일 책임 원칙(SRP) 준수
  - ✅ 도메인 경계 명확화 (회원 관리 vs 인증)
  - ✅ 테스트 용이성 향상
  - ✅ 재사용성 증가 (다른 회원 생성 시나리오에서도 활용 가능)

## 패키지 구조

```
auth/
├── config/          # SecurityConfig
├── entity/          # RefreshToken, EmailVerification
├── dto/             # SignupRequest, LoginRequest/Response, OAuth2Attributes
├── service/         # AuthService, EmailService, JwtService, CustomUserDetailsService, CustomOAuth2UserService
├── controller/      # AuthController
├── filter/          # JwtAuthenticationFilter
├── handler/         # OAuth2SuccessHandler
├── util/            # JwtTokenProvider
└── repository/      # RefreshTokenRepository, EmailVerificationRepository

member/
├── entity/          # Member (회원 엔티티)
│   └── type/        # AuthProvider, Role, MemberStatus (Enum)
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
