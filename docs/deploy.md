# 배포 가이드

## 배포 프로세스 개요

메인 서버는 GitHub Actions를 통해 자동으로 배포됩니다.

### 배포 트리거
- **브랜치:** `main`, `dev` 브랜치에 push 시 실행
- **실제 배포:** `main` 브랜치에 push 시에만 배포 (deploy job 조건: line 39)

### 배포 프로세스
1. **Test Job** (모든 브랜치)
   - JDK 17 설정
   - Gradle 캐시 설정
   - 테스트 실행 (`./gradlew test`)
   - 빌드 실행 (`./gradlew build -x test`)

2. **Deploy Job** (`main` 브랜치만)
   - Discord 배포 시작 알림
   - Docker 이미지 빌드 (multi-platform: amd64, arm64)
   - DockerHub에 이미지 push
   - EC2 서버에 SSH 접속하여 배포
   - Health Check 수행 (5회 재시도)
   - Discord 배포 결과 알림

## GitHub Actions Secrets 설정

### 현재 등록된 Secrets
- `AI_SERVER_HOST` - AI 서버 주소
- `AWS_ACCESS_KEY_ID` - AWS 인증 (IAM Role 사용 시 불필요)
- `AWS_SECRET_ACCESS_KEY` - AWS 인증 (IAM Role 사용 시 불필요)
- `DB_HOST` - RDS Private IP
- `DB_PASSWORD` - 데이터베이스 비밀번호
- `DISCORD_WEBHOOK` - Discord 알림 webhook URL
- `DOCKERHUB_TOKEN` - DockerHub 인증 토큰
- `DOCKERHUB_USERNAME` - DockerHub 사용자명
- `EC2_SSH_KEY` - EC2 SSH 접속용 private key
- `MAIN_SERVICE_URL` - 메인 서비스 URL
- `REDIS_HOST` - Redis Private IP

### deploy.yml에서 사용하지만 Secrets에 누락된 변수들

**⚠️ 다음 변수들을 GitHub Secrets에 추가해야 합니다:**
- `MAIN_SERVER_HOST` - EC2 서버 Public IP/도메인 (SSH 접속용)
- `DB_PORT` - 데이터베이스 포트 (일반적으로 3306)
- `DB_NAME` - 데이터베이스 이름
- `DB_USERNAME` - 데이터베이스 사용자명
- `REDIS_PORT` - Redis 포트 (일반적으로 6379)
- `JWT_SECRET` - JWT 토큰 서명용 시크릿
- `S3_BUCKET` - S3 버킷 이름 (예: mechuragi-dev-images)
- `AWS_REGION` - AWS 리전 (예: ap-northeast-2)
- `SES_FROM_EMAIL` - SES 발신 이메일 주소
- `KAKAO_CLIENT_ID` - 카카오 OAuth 클라이언트 ID
- `KAKAO_CLIENT_SECRET` - 카카오 OAuth 클라이언트 시크릿
- `KAKAO_REDIRECT_URI` - 카카오 OAuth 리다이렉트 URI
- `OAUTH2_REDIRECT_URI` - OAuth2 리다이렉트 URI

### 환경 변수 설정 확인사항

1. **AWS 인증 방식**
   - EC2에 IAM Role이 설정되어 있다면 `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` 불필요
   - IAM Role이 없다면 deploy.yml에 해당 환경 변수 추가 필요

2. **네트워크 설정**
   - `DB_HOST`, `REDIS_HOST`는 Private IP 사용 (VPC 내부 통신)
   - `MAIN_SERVER_HOST`는 Public IP/도메인 사용 (SSH 접속용)

3. **AI 서버 연동**
   - 메인 서버가 AI 서버와 통신하는 경우 `AI_SERVER_HOST` 환경 변수를 deploy.yml에 추가 필요

## Docker 컨테이너 설정

### 실행 포트
- `8080:8080` - Spring Boot 애플리케이션

### 재시작 정책
- `--restart unless-stopped` - 수동으로 중지하지 않는 한 항상 재시작

### 프로파일
- `SPRING_PROFILES_ACTIVE=dev` - 현재 dev 프로파일 사용 (line 104)

## Health Check

- **엔드포인트:** `http://localhost:8080/actuator/health`
- **대기 시간:** 45초
- **재시도:** 최대 5회 (10초 간격)
- **실패 시:** Docker 로그 출력 후 배포 실패 처리

## Discord 알림

배포 진행 상황이 Discord로 전송됩니다:
- 🚀 배포 시작
- ✅ 배포 성공
- ❌ 배포 실패 (오류 로그 링크 포함)

## 트러블슈팅

### 1. DB 연결 실패 (`Connection refused`)

**증상:**
```
java.net.ConnectException: Connection refused
```

**원인:**
- Public IP 사용 (VPC 내부에서는 Private IP 사용 필요)
- Security Group 설정 오류

**해결:**
```bash
# 1. Private IP 사용 확인
```

### 2. Redis 연결 실패

**해결:**
```bash
# Private IP 사용 확인
REDIS_HOST=  # ✅ 올바름
```

### 3. Health Check 실패

**증상:**
```
curl: (52) Empty reply from server
curl: (56) Recv failure: Connection reset by peer
```

**해결:**
```bash
# 1. 컨테이너 실행 확인
sudo docker ps | grep mechuragi-server

# 2. 로그 확인
sudo docker logs mechuragi-server

# 3. 컨테이너가 종료되었다면 재시작
sudo docker start mechuragi-server

# 4. 그래도 안되면 로그 전체 확인
sudo docker logs mechuragi-server 2>&1 | less
```

### 4. AWS SES 권한 오류

**증상:**
```
SesException: User is not authorized to perform: ses:SendEmail
```

**해결:**
1. EC2 IAM Role에 SES 권한 추가
2. SES에서 이메일 주소 인증
3. SES Production Access 요청 (Sandbox 모드 해제)

### 5. S3 업로드 실패

**해결:**
1. EC2 IAM Role에 S3 권한 추가:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:DeleteObject"
      ],
      "Resource": "arn:aws:s3:::mechuragi-dev-images/*"
    }
  ]
}
```