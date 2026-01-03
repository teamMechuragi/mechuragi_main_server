# Blue-Green 배포 구축 및 인프라 설정 트러블슈팅

## 테스트 일자: 2025-11-18

---

## 배포 환경
- 배포 방식: Blue-Green Deployment
- CI/CD: GitHub Actions
- 인프라: AWS EC2 (t3.small)
- 컨테이너: Docker + Docker Compose (standalone v1.29.2)
- 프록시: Nginx
- 데이터베이스: MySQL (VPC 내부 10.0.1.214:3306)
- 캐시: Redis (VPC 내부 10.0.1.185:6379)
- 브랜치: dev
- 관련 PR: #39, #40

---

## 배경

PR #39 머지 후 GitHub Actions에서 테스트 실패가 발생했고, 이를 해결하는 과정에서 dev 브랜치에 Blue-Green 배포 전략을 도입하기로 결정했습니다. 배포 과정에서 인프라 미설치, 환경 변수 누락, Docker 호환성 문제 등 여러 이슈를 발견하고 해결했습니다.

---

## 문제 해결 내역

### 문제 1: GitHub Actions 테스트 실패 - Redis 연결 실패
**발생 일시:** 2025-11-18

**에러 메시지:**
```
Unable to connect to Redis on localhost:6379
org.springframework.data.redis.RedisConnectionFailureException: Unable to connect to Redis
```

**원인 분석:**
- PR #39에서 Redis Pub/Sub 기능이 추가됨
- 통합 테스트가 Redis 연결을 필요로 함
- GitHub Actions의 test job에 Redis service container가 없음

**해결 방법:**
GitHub Actions workflow에 Redis service container 추가

**수정 파일:**
`.github/workflows/deploy.yml:9-18`

**수정 내용:**
```yaml
services:
  redis:
    image: redis:latest
    ports:
      - 6379:6379
    options: >-
      --health-cmd "redis-cli ping"
      --health-interval 10s
      --health-timeout 5s
      --health-retries 5
```

**결과:**
- ✅ 테스트 job에서 Redis 통합 테스트 정상 동작
- ✅ CI/CD 파이프라인 안정화

---

### 문제 2: VotePostServiceTest 실패 - Mock 객체 상호작용 없음
**발생 일시:** 2025-11-18

**에러 메시지:**
```
Wanted but not invoked:
memberRepository.findById(1L);
Actually, there were zero interactions with this mock.
```

**원인 분석:**
1. PR #39의 커밋 `deda812`에서 `VotePostService`에 `memberRepository`와 `notificationService` 의존성 추가
2. 투표 종료 알림 기능 구현 시 회원 조회 로직 추가됨
3. 기존 테스트 코드가 새로운 의존성을 Mock 처리하지 않음
4. 테스트 실행 시 NullPointerException 또는 Mock 상호작용 실패 발생

**해결 방법:**
테스트 코드에 누락된 Mock 객체와 setUp 로직 추가

**수정 파일:**
`src/test/java/com/mechuragi/mechuragi_server/domain/vote/service/VotePostServiceTest.java`

**수정 내용:**

1. **Mock 객체 추가**
```java
@Mock
private MemberRepository memberRepository;

@Mock
private NotificationService notificationService;
```

2. **setUp 메서드 수정**
```java
@BeforeEach
void setUp() {
    testMember = Member.builder()
            .email("test@example.com")
            .nickname("테스터")
            .build();

    ReflectionTestUtils.setField(testMember, "id", 1L);
    ReflectionTestUtils.setField(testMember, "voteNotificationEnabled", true);
}
```

3. **테스트 메서드에 Mock 설정 추가**
```java
@Test
@DisplayName("투표 종료 10분 전 알림 발행 성공")
void notifyVoteEndingSoon_Success() {
    when(votePostRepository.findById(voteId)).thenReturn(Optional.of(testVotePost));
    when(memberRepository.findById(testMember.getId())).thenReturn(Optional.of(testMember));

    votePostService.notifyVoteEndingSoon(voteId, title);

    verify(redisTemplate, times(1)).convertAndSend(
        eq("vote-notification"),
        any(VoteNotificationMessageDTO.class)
    );
}
```

**결과:**
- ✅ VotePostServiceTest 100% 통과
- ✅ 신규 알림 기능 테스트 커버리지 확보

**교훈:**
- 서비스 계층 의존성 추가 시 테스트 코드도 함께 업데이트 필요
- Mock 객체 누락은 런타임 에러가 아닌 테스트 실패로 나타남

---

### 문제 3: 통합 테스트 실패 - Redis 포트 불일치
**발생 일시:** 2025-11-18

**에러 메시지:**
```
Failed to load ApplicationContext
Unable to connect to Redis on localhost:6370
```

**원인 분석:**
- 테스트 설정 파일(`application-test.yml`)에 Redis 포트가 6370으로 설정됨
- GitHub Actions의 Redis service container는 6379 포트 사용
- 로컬 개발 환경의 Redis도 6379 포트 사용
- 포트 불일치로 통합 테스트 실패

**해결 방법:**
테스트 설정 파일의 Redis 포트를 표준 포트 6379로 수정

**수정 파일:**
`src/test/resources/application-test.yml:7-9`

**수정 내용:**
```yaml
# 변경 전
spring:
  data:
    redis:
      host: localhost
      port: 6370

# 변경 후
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

**결과:**
- ✅ 로컬 및 CI 환경에서 통합 테스트 정상 동작
- ✅ Redis 연결 설정 통일

**교훈:**
- 테스트 환경 설정은 표준 포트 사용 권장
- 특별한 이유 없이 비표준 포트 사용 지양

---

### 문제 4: Blue-Green 배포 스크립트 실패 - docker compose 명령어 미지원
**발생 일시:** 2025-11-18

**에러 메시지:**
```
docker: unknown command: docker compose
See 'docker --help'
```

**원인 분석:**
- EC2 인스턴스에 `docker-compose` (standalone v1.29.2) 설치됨
- 배포 스크립트는 `docker compose` (plugin v2) 명령어 사용
- Docker Compose v2는 plugin 형태로 `docker compose` 사용
- Docker Compose v1은 standalone 바이너리로 `docker-compose` 사용
- 서버 환경이 v1만 지원

**해결 방법:**
Blue-Green 배포 스크립트의 모든 `docker compose` 명령어를 `docker-compose`로 변경

**수정 파일:**
`scripts/blue-green-deploy.sh`

**수정 내용:**
```bash
# 변경 전
docker compose -f docker-compose.blue-green.yml up -d ${PROJECT_NAME}-main-${new_active}

# 변경 후
docker-compose -f docker-compose.blue-green.yml up -d ${PROJECT_NAME}-main-${new_active}
```

**전체 변경 위치:**
- 컨테이너 시작 명령어
- 컨테이너 중지 명령어
- 컨테이너 조회 명령어

**결과:**
- ✅ Blue-Green 배포 스크립트 정상 실행
- ✅ 컨테이너 자동 교체 동작

**교훈:**
- 서버 환경의 Docker 버전 확인 필수
- 신규 문법 사용 시 호환성 고려 필요
- 또는 Docker Compose v2 설치를 고려할 수 있으나, 기존 스크립트 수정이 더 간단함

---

### 문제 5: 애플리케이션 시작 실패 - MySQL 연결 거부
**발생 일시:** 2025-11-18

**에러 메시지:**
```
Communications link failure
The last packet sent successfully to the server was 0 milliseconds ago.
The driver has not received any packets from the server.
com.mysql.cj.jdbc.exceptions.CommunicationsException: Communications link failure
java.net.ConnectException: Connection refused (Connection refused)
```

**원인 분석:**
1. 애플리케이션 설정에 MySQL 연결 정보 존재 (`DB_HOST=10.0.1.214:3306`)
2. MySQL EC2 인스턴스(10.0.1.214)에 MySQL 서버 미설치
3. 인프라 구축 시 MySQL 설치 누락

**해결 방법:**
Ansible playbook을 사용하여 MySQL 서버 자동 설치 및 설정

**실행 명령어:**
```bash
cd /Users/faker/IdeaProjects/mechuragi_infra/ansible
ansible-playbook -i inventory/hosts.yml playbooks/setup/setup-database.yml
```

**Ansible 작업 내용:**
1. MySQL 8.0 설치
2. MySQL 서비스 시작 및 활성화
3. `bind-address = 0.0.0.0` 설정 (VPC 내부 접속 허용)
4. 데이터베이스 및 사용자 생성
5. 권한 부여

**관련 파일:**
- `ansible/playbooks/setup/setup-database.yml`
- `ansible/roles/mysql/tasks/main.yml`

**결과:**
- ✅ MySQL 서버 정상 설치 및 실행
- ✅ VPC 내부에서 애플리케이션 연결 성공
- ✅ 데이터베이스 초기화 완료

**교훈:**
- Infrastructure as Code (Ansible) 활용으로 수동 설치 오류 방지
- 인프라 구축 체크리스트 작성 필요

---

### 문제 6: 애플리케이션 시작 실패 - 환경 변수 플레이스홀더 해석 불가
**발생 일시:** 2025-11-18

**에러 메시지:**
```
Could not resolve placeholder 'OAUTH2_REDIRECT_URI' in value "${OAUTH2_REDIRECT_URI}"
```

**원인 분석:**
1. PR #40에서 OAuth2 및 카카오 로그인 설정 추가됨
2. `application-dev.yml`에 환경 변수 플레이스홀더 추가됨
3. GitHub Actions workflow에 해당 환경 변수 미설정
4. `docker-compose.blue-green.yml`에도 환경 변수 미설정
5. 총 5개 환경 변수 누락:
   - `OAUTH2_REDIRECT_URI`
   - `KAKAO_CLIENT_ID`
   - `KAKAO_CLIENT_SECRET`
   - `KAKAO_REDIRECT_URI`
   - `AI_SERVICE_URL`

**해결 방법:**
GitHub Actions workflow와 Docker Compose 파일에 누락된 환경 변수 추가

**수정 파일:**

1. **`.github/workflows/deploy.yml:143-147`**
```yaml
export OAUTH2_REDIRECT_URI="${{ secrets.OAUTH2_REDIRECT_URI }}"
export KAKAO_CLIENT_ID="${{ secrets.KAKAO_CLIENT_ID }}"
export KAKAO_CLIENT_SECRET="${{ secrets.KAKAO_CLIENT_SECRET }}"
export KAKAO_REDIRECT_URI="${{ secrets.KAKAO_REDIRECT_URI }}"
export AI_SERVICE_URL="${{ secrets.AI_SERVICE_URL }}"
```

2. **`docker-compose.blue-green.yml:24-28, 59-63`**
```yaml
# Blue 컨테이너
environment:
  - OAUTH2_REDIRECT_URI=${OAUTH2_REDIRECT_URI}
  - KAKAO_CLIENT_ID=${KAKAO_CLIENT_ID}
  - KAKAO_CLIENT_SECRET=${KAKAO_CLIENT_SECRET}
  - KAKAO_REDIRECT_URI=${KAKAO_REDIRECT_URI}
  - AI_SERVICE_URL=${AI_SERVICE_URL}

# Green 컨테이너 (동일하게 추가)
```

**결과:**
- ✅ OAuth2 관련 환경 변수 정상 주입
- ✅ 애플리케이션 시작 성공
- ✅ 카카오 로그인 기능 활성화

**교훈:**
- 새로운 기능 추가 시 환경 변수 체크리스트 관리 필요
- GitHub Secrets와 배포 파일 간 동기화 확인
- `application.yml` 변경 시 배포 설정도 함께 검토

---

### 문제 7: 애플리케이션 크래시 루프 - Redis 연결 실패
**발생 일시:** 2025-11-18

**에러 메시지:**
```
Unable to connect to Redis on 10.0.1.185:6379
org.springframework.data.redis.RedisConnectionFailureException: Unable to connect to Redis
```

**Docker 로그:**
```
Caused by: java.net.ConnectException: Connection refused
at java.base/sun.nio.ch.Net.pollConnect(Native Method)
```

**원인 분석:**
1. 애플리케이션 설정에 Redis 연결 정보 존재 (`REDIS_HOST=10.0.1.185`)
2. Redis EC2 인스턴스(10.0.1.185)에 Redis 서버 미설치
3. 애플리케이션이 Redis 연결 실패로 반복적으로 재시작
4. 인프라 구축 시 Redis 설치 누락

**해결 방법:**
Ansible playbook을 사용하여 Redis 서버 자동 설치 및 설정

**실행 명령어:**
```bash
cd /Users/faker/IdeaProjects/mechuragi_infra/ansible
ansible-playbook -i inventory/hosts.yml playbooks/setup/setup-cache.yml
```

**Ansible 작업 내용:**
1. Redis 6.0.16 설치
2. Redis 서비스 시작 및 활성화
3. `bind 0.0.0.0` 설정 (VPC 내부 접속 허용)
4. Redis 백업 설정

**관련 파일:**
- `ansible/playbooks/setup/setup-cache.yml`
- `ansible/roles/redis/tasks/main.yml`

**결과:**
- ✅ Redis 서버 정상 설치 및 실행
- ✅ 애플리케이션 Redis 연결 성공
- ✅ 크래시 루프 해결, 컨테이너 안정화

**교훈:**
- 분산 시스템에서 모든 의존성 서버 사전 설치 확인 필수
- 연결 실패 시 재시도 로직 고려 필요
- Ansible로 인프라를 코드화하여 설치 누락 방지

---

### 문제 8: 배포 실패 - EC2 인스턴스 중지 상태
**발생 일시:** 2025-11-18

**에러 메시지:**
```
ssh: connect to host *** port 22: Operation timed out
dial tcp ***:22: i/o timeout
```

**원인 분석:**
- GitHub Actions SSH 연결 시도 시 타임아웃 발생
- EC2 인스턴스가 중지(stopped) 상태였음
- 비용 절감을 위해 인스턴스를 수동으로 중지했었음

**해결 방법:**
1. AWS Console에서 EC2 인스턴스 시작
2. 인스턴스 상태가 `running`으로 변경될 때까지 대기
3. SSH 연결 가능 확인
4. GitHub Actions 재실행

**결과:**
- ✅ 인스턴스 재시작 후 SSH 연결 성공
- ✅ 배포 프로세스 정상 진행

**교훈:**
- 배포 전 인스턴스 상태 확인 필요
- 자동화된 인스턴스 시작/중지 스케줄 고려
- 또는 배포 스크립트에 인스턴스 상태 체크 로직 추가

---

### 문제 9: Docker 컨테이너 생성 실패 - KeyError: 'ContainerConfig'
**발생 일시:** 2025-11-18

**에러 메시지:**
```
Error response from daemon: error creating container: KeyError: 'ContainerConfig'
```

**원인 분석:**
1. 이전 배포 실패로 Green 컨테이너가 비정상 상태로 남아있음
2. Docker 메타데이터가 손상된 상태의 컨테이너
3. `docker-compose up` 시 기존 컨테이너 재사용 시도
4. 손상된 메타데이터로 인해 컨테이너 생성 실패

**해결 방법:**
손상된 컨테이너를 강제 삭제

**실행 명령어:**
```bash
# SSH로 EC2 접속
ssh ubuntu@[EC2_IP]

# 문제 컨테이너 강제 삭제
docker rm -f mechuragi-main-green

# 배포 재실행
./scripts/blue-green-deploy.sh
```

**결과:**
- ✅ 컨테이너 정상 생성
- ✅ Blue-Green 배포 성공

**교훈:**
- 배포 실패 시 좀비 컨테이너 정리 필요
- Blue-Green 스크립트에 컨테이너 정리 로직 강화 고려
- `docker rm -f`로 강제 삭제 후 재생성이 안전

---

### 문제 10: 도메인 접속 불가 - 504 Gateway Timeout
**발생 일시:** 2025-11-18

**증상:**
```
https://mechuragi.kro.kr/api/actuator/health 접속 시
504 Gateway Timeout ERROR
```

**원인 분석:**
1. Docker 컨테이너는 정상 실행 중 (포트 8081)
2. EC2 내부에서 `curl http://localhost:8081/actuator/health` 정상 응답
3. 외부 도메인 접속 시 504 에러
4. Nginx가 EC2 인스턴스에 설치되지 않음
5. CloudFront → EC2로 직접 연결 시도하여 프록시 없음

**해결 방법:**
Ansible playbook을 사용하여 Nginx 자동 설치 및 Blue-Green 설정

**실행 명령어:**
```bash
cd /Users/faker/IdeaProjects/mechuragi_infra/ansible
ansible-playbook -i inventory/hosts.yml playbooks/setup/setup-nginx.yml
```

**Ansible 작업 내용:**
1. Nginx 설치
2. Nginx 서비스 시작 및 활성화
3. Blue-Green Nginx 설정 파일 배포
4. 기본 사이트 비활성화
5. Nginx 설정 테스트 및 재시작

**관련 파일:**
- `ansible/playbooks/setup/setup-nginx.yml`
- `ansible/roles/nginx/templates/nginx-main-service.conf.j2`

**Nginx 설정 (주요 부분):**
```nginx
upstream backend {
    server localhost:8080;  # Blue 또는 Green 포트
}

server {
    listen 80;
    server_name mechuragi.kro.kr;

    location /api/ {
        proxy_pass http://backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

**결과:**
- ✅ Nginx 정상 설치 및 실행
- ✅ 도메인 접속 성공
- ✅ 프록시 기능 정상 동작

**교훈:**
- Blue-Green 배포는 Nginx 프록시 필수
- 인프라 사전 설치 항목: MySQL, Redis, Nginx
- Ansible로 인프라 자동화 필수

---

### 문제 11: Nginx 프록시 오류 - 포트 불일치
**발생 일시:** 2025-11-18

**증상:**
- Blue-Green 배포 완료
- 현재 활성 컨테이너: Green (포트 8081)
- Nginx upstream: `localhost:8080` (Blue)
- 외부 접속 시 연결 실패

**원인 분석:**
1. Blue-Green 배포 스크립트가 Green 컨테이너 활성화 (8081)
2. Nginx 설정은 초기 템플릿 그대로 8080 포트 사용
3. Nginx가 비활성 컨테이너(Blue)로 프록시 시도
4. 포트 불일치로 프록시 실패

**해결 방법:**
Nginx upstream 설정을 현재 활성 컨테이너 포트(8081)로 수정

**수정 명령어:**
```bash
# SSH로 EC2 접속
ssh ubuntu@[EC2_IP]

# Nginx 설정 수정
sudo vi /etc/nginx/sites-available/main-service

# upstream 포트 변경
upstream backend {
    server localhost:8081;  # 8080 → 8081
}

# Nginx 설정 테스트
sudo nginx -t

# Nginx 재시작
sudo systemctl restart nginx
```

**결과:**
- ✅ Nginx 프록시 정상 동작
- ✅ 도메인 헬스체크 성공
- ✅ Blue-Green 배포 완전 성공

**Blue-Green 배포 포트 전환 규칙:**
```
배포 전:
- Blue (8080) ← Nginx 프록시 ✅ 활성
- Green (8081) ❌ 비활성

배포 중:
1. Green (8081) 컨테이너 시작
2. Green 헬스체크 대기
3. Nginx upstream을 8081로 변경
4. Blue (8080) 컨테이너 중지

배포 후:
- Blue (8080) ❌ 비활성
- Green (8081) ← Nginx 프록시 ✅ 활성
```

**개선 필요 사항:**
Blue-Green 배포 스크립트에 Nginx upstream 자동 변경 로직 추가 필요

**참고 파일:**
- `scripts/blue-green-deploy.sh` - 배포 스크립트
- `/etc/nginx/sites-available/main-service` - Nginx 설정

**교훈:**
- Blue-Green 배포 시 프록시 설정 자동화 필수
- 수동 포트 변경은 휴먼 에러 가능성 높음
- 배포 스크립트에 Nginx 설정 업데이트 로직 추가 검토

---

## Blue-Green 배포 아키텍처

### 최종 구성

```
[GitHub Actions]
    ↓ (Docker 이미지 빌드 & 푸시)
[DockerHub]
    ↓ (이미지 pull)
[EC2 인스턴스]
    ├── Nginx (80) → Blue (8080) 또는 Green (8081)
    ├── Blue Container (8080)
    ├── Green Container (8081)
    ├── MySQL (10.0.1.214:3306) - VPC 내부
    └── Redis (10.0.1.185:6379) - VPC 내부

[CloudFront] → [EC2:80] → [Nginx] → [Blue/Green]
```

### 배포 플로우

1. **코드 푸시**: dev 브랜치에 푸시
2. **테스트**: GitHub Actions에서 자동 테스트 (Redis 포함)
3. **빌드**: Docker 이미지 빌드 및 DockerHub 푸시
4. **파일 전송**: 배포 스크립트 및 설정 파일 EC2 업로드
5. **Blue-Green 배포**:
   - 현재 비활성 컨테이너(Blue/Green) 시작
   - 헬스체크 대기 (최대 30회 재시도)
   - Nginx upstream 포트 변경
   - 이전 활성 컨테이너 중지
6. **검증**: 외부 도메인 헬스체크

### 주요 설정 파일

- `.github/workflows/deploy.yml` - CI/CD 파이프라인
- `docker-compose.blue-green.yml` - Blue/Green 컨테이너 정의
- `scripts/blue-green-deploy.sh` - 배포 자동화 스크립트
- `nginx/blue-green.conf` - Nginx 프록시 설정

---

## 인프라 구축 체크리스트

### EC2 인스턴스 사전 설치 항목

- ✅ Docker & Docker Compose
- ✅ MySQL 8.0 (Ansible: `setup-database.yml`)
- ✅ Redis 6.0.16 (Ansible: `setup-cache.yml`)
- ✅ Nginx (Ansible: `setup-nginx.yml`)

### GitHub Secrets 필수 항목

- ✅ `DOCKERHUB_USERNAME` - DockerHub 사용자명
- ✅ `DOCKERHUB_TOKEN` - DockerHub 액세스 토큰
- ✅ `MAIN_SERVER_HOST` - EC2 공인 IP
- ✅ `EC2_SSH_KEY` - EC2 SSH 개인키
- ✅ `DB_HOST`, `DB_PORT`, `DB_NAME` - MySQL 연결 정보
- ✅ `DB_USERNAME`, `DB_PASSWORD` - MySQL 계정 정보
- ✅ `REDIS_HOST`, `REDIS_PORT` - Redis 연결 정보
- ✅ `JWT_SECRET` - JWT 암호화 키
- ✅ `AWS_REGION`, `S3_BUCKET`, `SES_FROM_EMAIL` - AWS 설정
- ✅ `BEDROCK_AI_HOST` - AI 서비스 호스트
- ✅ `OAUTH2_REDIRECT_URI` - OAuth2 리다이렉트 URI
- ✅ `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET` - 카카오 로그인
- ✅ `KAKAO_REDIRECT_URI` - 카카오 리다이렉트 URI
- ✅ `AI_SERVICE_URL` - AI 서비스 URL

### 환경 변수 동기화 확인

1. GitHub Secrets 등록
2. `.github/workflows/deploy.yml`에 export 추가
3. `docker-compose.blue-green.yml`에 environment 추가

---

## 참고사항

### Docker Compose 버전
- 서버: `docker-compose` (standalone v1.29.2)
- 로컬: `docker compose` (plugin v2) - 서버와 다름 주의

### Blue-Green 포트
- Blue: 8080
- Green: 8081
- Nginx: 80 (외부 접속)

### VPC 내부 통신
- MySQL: 10.0.1.214:3306
- Redis: 10.0.1.185:6379
- Security Group에서 VPC CIDR 허용 필수

### 헬스체크
- Endpoint: `/actuator/health`
- 타임아웃: 30회 재시도 (약 60초)
- 실패 시 배포 롤백

---

## 총 해결된 문제 수: 11개

1. ✅ Redis 서비스 누락 (GitHub Actions)
2. ✅ Mock 객체 누락 (VotePostServiceTest)
3. ✅ Redis 포트 불일치 (application-test.yml)
4. ✅ Docker Compose 명령어 호환성 (v1 vs v2)
5. ✅ MySQL 서버 미설치
6. ✅ OAuth2/Kakao 환경 변수 누락 (5개)
7. ✅ Redis 서버 미설치
8. ✅ EC2 인스턴스 중지 상태
9. ✅ Docker 컨테이너 메타데이터 손상
10. ✅ Nginx 미설치
11. ✅ Nginx 포트 불일치

**최종 결과:** Blue-Green 배포 성공, dev 브랜치 자동 배포 활성화

---
