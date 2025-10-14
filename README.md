# Mechuragi Main Server 프로젝트 구조

## 1. 프로젝트 개요
Spring Boot 3.5.5 기반의 백엔드 서버로, Docker 컨테이너로 배포되는 메인 서비스입니다.

## 2. 인프라 구성
- **메인 서비스**: t3.micro (Public IP + Elastic IP) - 현재 프로젝트
- **AI 서비스**: t3.small (Bedrock IAM Profile)
- **MySQL 서버**: t3.micro (30GB gp3 볼륨)
- **Redis 서버**: t3.micro (캐시 전용)

## 3. 기술 스택
- **언어**: Java 17
- **프레임워크**: Spring Boot 3.5.5
- **빌드 도구**: Gradle
- **데이터베이스**: MySQL 8.0
- **캐시**: Redis
- **컨테이너**: Docker, Docker Compose

## 4. 주요 의존성
- Spring Boot Starter Web
- Spring Data JPA
- Spring Data Redis
- Spring Boot Actuator
- Spring Validation
- MySQL Connector
- Spring Dotenv (환경 변수 관리)

## 5. 디렉토리 구조
```
mechuragi_main_server/
├── src/
│   ├── main/
│   │   ├── java/com/mechuragi/mechuragi_server/
│   │   │   ├── MechuragiServerApplication.java    # 메인 애플리케이션
│   │   │   ├── TestController.java                # 테스트 API 컨트롤러
│   │   │   ├── domain/                            # 도메인 패키지
│   │   │   │   ├── member/                        # 회원 도메인
│   │   │   │   │   ├── entity/                    # Member (회원 엔티티)
│   │   │   │   │   ├── repository/                # MemberRepository
│   │   │   │   │   ├── service/                   # MemberService (예정)
│   │   │   │   │   ├── controller/                # MemberController (예정)
│   │   │   │   │   └── dto/                       # MemberRequest, MemberResponse (예정)
│   │   │   │   └── preference/                    # 음식 취향 도메인
│   │   │   │       ├── entity/                    # FoodPreference, DislikedFood 등
│   │   │   │       ├── repository/                # FoodPreferenceRepository 등
│   │   │   │       ├── service/                   # FoodPreferenceService
│   │   │   │       ├── controller/                # FoodPreferenceController
│   │   │   │       └── dto/                       # CreatePreferenceRequest 등
│   │   │   └── auth/                              # 모든 도메인에 작동하는 전역 인증 인프라
│   │   │       ├── config/                        # SecurityConfig, JwtConfig
│   │   │       ├── entity/                        # RefreshToken, EmailVerification
│   │   │       ├── dto/                           # SignupRequest, LoginRequest/Response
│   │   │       ├── service/                       # AuthService, EmailService, JwtService
│   │   │       ├── controller/                    # AuthController
│   │   │       └── filter/                        # JwtAuthenticationFilter
│   │   └── resources/
│   │       ├── application.yml                    # 공통 설정
│   │       ├── application-local.yml              # 로컬 환경 설정
│   │       └── application-docker.yml             # Docker 환경 설정
│   └── test/
│       ├── java/com/mechuragi/mechuragi_server/
│       │   └── MechuragiServerApplicationTests.java
│       └── resources/
│           └── application-test.yml
├── gradle/                                        # Gradle wrapper
├── Dockerfile                                     # 멀티 스테이지 빌드 설정
├── docker-compose.yml                             # 전체 스택 구성
├── build.gradle                                   # 빌드 설정
├── settings.gradle                                # 프로젝트 설정
├── gradlew                                        # Gradle wrapper 스크립트
└── gradlew.bat                                    # Windows용 Gradle wrapper
```

## 6. Docker 구성

### 6.1. Dockerfile
- **빌드 스테이지**: eclipse-temurin:17-jdk 사용
  - Gradle 의존성 캐싱
  - 소스 코드 빌드 (테스트 제외)
- **실행 스테이지**: eclipse-temurin:17-jre 사용
  - 한국 시간대 설정 (Asia/Seoul)
  - 보안을 위한 전용 사용자(spring) 생성
  - JVM 메모리 옵션: MaxRAMPercentage=75.0
  - 헬스체크: `/actuator/health` 엔드포인트
  - 포트: 8080

### 6.2. docker-compose.yml
4개의 서비스로 구성:

1. **spring-app** (메인 백엔드)
   - 컨테이너명: mechuragi-server-backend
   - 포트: 8080
   - 프로파일: docker
   - 의존성: mysql, redis

2. **nginx** (웹 서버)
   - 컨테이너명: mechuragi-server-nginx
   - 포트: 80
   - 역할: 리버스 프록시 및 정적 파일 서빙

3. **mysql** (데이터베이스)
   - 컨테이너명: mechuragi-server-mysql
   - MySQL 8.0
   - 볼륨: mysql_data (영구 저장)
   - 초기화 스크립트: init.sql

4. **redis** (캐시)
   - 컨테이너명: mechuragi-server-redis
   - 포트: 6379
   - AOF 영속성 활성화
   - 볼륨: redis_data

### 6.3. 네트워크
- **app-network**: bridge 드라이버 사용하여 모든 서비스 연결

## 7. 환경별 설정

### 7.1. application.yml (공통)
- 서버 포트: 8080
- JPA: Hibernate ddl-auto=update, MySQL Dialect
- 파일 업로드: 최대 10MB
- JWT 만료 시간: 86400000ms (24시간)
- Actuator: health, info 엔드포인트 노출
- AWS S3: 버킷(mechuragi-dev-images), 리전(ap-northeast-2)

### 7.2. application-local.yml (로컬 개발)
- MySQL: localhost:3306
- Redis: localhost:6379
- JPA SQL 로깅: 활성화 (포맷팅 포함)
- 로깅 레벨: DEBUG

### 7.3. application-docker.yml (Docker 환경)
- MySQL/Redis: 환경 변수로 주입
- JPA SQL 로깅: 비활성화
- 프로덕션 최적화 설정

## 8. API 엔드포인트

### 8.1. TestController
- `GET /api/test`: 서버 상태 테스트
  - 응답: message, timestamp, status
- `GET /api/health`: 서비스 헬스 체크
  - 응답: status, service

### 8.2. Spring Actuator
- `/actuator/health`: 상세 헬스 체크 정보
- `/actuator/info`: 애플리케이션 정보

## 9. 환경 변수
Docker Compose에서 필요한 환경 변수:
- `DB_NAME`: 데이터베이스 이름
- `DB_USERNAME`: 데이터베이스 사용자명
- `DB_PASSWORD`: 데이터베이스 비밀번호
- `MYSQL_ROOT_PASSWORD`: MySQL root 비밀번호
- `REDIS_PORT`: Redis 포트 (기본: 6379)
- `JWT_SECRET`: JWT 시크릿 키

## 10. 빌드 및 실행

### 10.1. 로컬 개발
```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

### 10.2. Docker 빌드 및 실행
```bash
docker-compose up -d
```

### 10.3. Docker 중지
```bash
docker-compose down
```

## 11. 보안 및 최적화
- Spring Security 및 OAuth2 설정 준비됨 (주석 처리)
- 멀티 스테이지 빌드로 이미지 크기 최소화
- 비 root 사용자로 컨테이너 실행
- 컨테이너 메모리 자동 감지 및 제한
- 헬스체크 자동화
- 재시작 정책: unless-stopped

## 12. CI/CD 파이프라인

### 12.1. GitHub Actions 워크플로우
자동 배포 파이프라인이 구성되어 있습니다.

#### 12.1.1. 트리거
- **main 브랜치 push**: 테스트 + 빌드 + 배포
- **main/dev 브랜치 PR**: 테스트 + 빌드만 실행

#### 12.1.2. 배포 프로세스
1. **테스트 단계**
   - JDK 17 설정
   - Gradle 캐시 활용
   - 단위 테스트 실행
   - 애플리케이션 빌드

2. **배포 단계** (main 브랜치만)
   - Docker 이미지 빌드 (멀티 플랫폼: amd64, arm64)
   - DockerHub에 이미지 푸시 (mamel1016/mechuragi-app:latest)
   - EC2 서버 SSH 접속
   - 기존 컨테이너 중지 및 제거
   - 새 컨테이너 실행 (환경 변수 주입)
   - 헬스체크 확인
   - 사용하지 않는 이미지 정리

#### 12.1.3. 필요한 GitHub Secrets
- `DOCKERHUB_TOKEN`: DockerHub 접근 토큰
- `EC2_SSH_KEY`: EC2 서버 SSH 키
- `DB_HOST`, `DB_PORT`, `DB_NAME`: 데이터베이스 연결 정보
- `DB_USERNAME`, `DB_PASSWORD`: 데이터베이스 인증 정보
- `REDIS_HOST`, `REDIS_PORT`: Redis 연결 정보
- `JWT_SECRET`: JWT 시크릿 키
- `S3_BUCKET`, `AWS_REGION`: AWS S3 설정

#### 12.1.4. 배포 서버
- **호스트**: 
- **포트**: 8080
- **컨테이너명**: mechuragi-app
- **헬스체크**: http://localhost:8080/actuator/health