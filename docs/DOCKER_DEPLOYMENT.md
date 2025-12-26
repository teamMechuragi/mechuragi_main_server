# 메추라기 서버 Docker 배포 가이드

## 📁 파일 구조

```
mechuragi_server/
├── docker-compose.yml                    # 로컬 개발용 ✅
├── docker-compose.prod.yml               # 프로덕션용 (통합 메인 서버) ✅
├── docker-compose.blue-green.yml.backup  # 백업 (기존 구조)
├── Dockerfile                            # API 이미지 빌드용
├── .env                                  # 로컬 개발 환경변수
├── .env.dev                              # 프로덕션 환경변수
└── DOCKER_DEPLOYMENT.md                  # 이 문서
```

---

## 🏗️ 인프라 구조

### 새 인프라 (3개 인스턴스 통합)
```
┌─────────────────────────────────────────────────────────┐
│  통합 메인 서버 (<MAIN_SERVER_IP> - Private Subnet)      │
│  ┌─────────────────────────────────────────────────┐    │
│  │  docker-compose.prod.yml                        │    │
│  │  ┌──────────┐  ┌────────┐  ┌────────────────┐  │    │
│  │  │  MySQL   │  │ Redis  │  │ Spring Boot    │  │    │
│  │  │  :3306   │  │ :6379  │  │ Blue   :8080   │  │    │
│  │  │          │  │        │  │ Green  :8081   │  │    │
│  │  └──────────┘  └────────┘  └────────────────┘  │    │
│  └─────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────┘
           ▲                          ▲
           │ MySQL                    │ API
           │                          │
      ┌────┴──────┐          ┌────────┴────────┐
      │ AI 서버    │          │ Nginx Gateway   │
      │<AI_SERVER_IP>│        │<GATEWAY_IP>     │
      └───────────┘          └─────────────────┘
```

---

## 🚀 사용 방법

### 1. 로컬 개발 (docker-compose.yml)

**용도**: 개발자 로컬 환경에서 전체 스택 실행

```bash
# .env 파일 확인 (로컬용)
cat .env

# 전체 서비스 실행 (MySQL + Redis + Spring Boot + Nginx)
docker-compose up -d

# 로그 확인
docker-compose logs -f

# 서비스 중지
docker-compose down

# 데이터 포함 완전 삭제
docker-compose down -v
```

**특징**:
- MySQL, Redis, Spring Boot, Nginx 모두 포함
- localhost에서 모든 서비스 접근 가능
- 포트: 80 (Nginx), 8080 (Spring Boot), 3306 (MySQL), 6379 (Redis)
- MYSQL_ROOT_PASSWORD: `.env` 파일 참조 (개인용)

---

### 2. 프로덕션 배포 (docker-compose.prod.yml)

**용도**: 통합 메인 서버 (<MAIN_SERVER_IP>)에서 MySQL + Redis + API 통합 관리

#### 2-1. 일반 배포 (Blue만 실행)

```bash
# 서버 접속 (Bastion 경유)
ssh -J ubuntu@<GATEWAY_IP> ubuntu@<MAIN_SERVER_IP>

# 프로젝트 디렉토리로 이동
cd ~/mechuragi_server

# .env.dev 파일 확인 (프로덕션용)
cat .env.dev

# Blue 버전만 실행
docker-compose -f docker-compose.prod.yml up -d

# 상태 확인
docker-compose -f docker-compose.prod.yml ps

# 헬스체크 확인
curl http://localhost:8080/actuator/health

# 로그 확인
docker-compose -f docker-compose.prod.yml logs -f mechuragi-main-blue
```

**특징**:
- MySQL + Redis + Spring Boot 통합
- 컨테이너 간 localhost 통신
- MYSQL_ROOT_PASSWORD: `.env.dev` 파일 참조 (인프라 가이드)

#### 2-2. Blue-Green 무중단 배포

**Step 1: 새 이미지 빌드 및 푸시**
```bash
# 로컬 개발 환경에서
docker build -t <DOCKERHUB_USERNAME>/mechuragi-app:latest .
docker push <DOCKERHUB_USERNAME>/mechuragi-app:latest
```

**Step 2: Green 버전 시작**
```bash
# 통합 메인 서버에서
ssh -J ubuntu@<GATEWAY_IP> ubuntu@<MAIN_SERVER_IP>
cd ~/mechuragi_server

# 최신 이미지 가져오기
docker pull <DOCKERHUB_USERNAME>/mechuragi-app:latest

# Green 버전 실행 (Blue는 계속 실행 중)
docker-compose -f docker-compose.prod.yml --profile deploy up -d mechuragi-main-green

# Green 헬스체크 (준비 완료까지 약 60초)
watch curl http://localhost:8081/actuator/health
```

**Step 3: Nginx 트래픽 전환**
```bash
# Nginx Gateway 서버에서 upstream 변경
ssh ubuntu@<GATEWAY_IP>

# Nginx 설정 파일 확인
sudo cat /etc/nginx/conf.d/proxy.conf

# upstream을 8080 (Blue) → 8081 (Green)으로 변경
sudo vi /etc/nginx/conf.d/proxy.conf

# 설정 테스트
sudo nginx -t

# Nginx 리로드 (무중단)
sudo nginx -s reload
```

**Step 4: Blue 버전 종료**
```bash
# 통합 메인 서버로 돌아와서
ssh -J ubuntu@<GATEWAY_IP> ubuntu@<MAIN_SERVER_IP>

# 트래픽이 Green으로 완전히 전환된 후 Blue 종료
docker-compose -f docker-compose.prod.yml stop mechuragi-main-blue
docker-compose -f docker-compose.prod.yml rm -f mechuragi-main-blue
```

**Step 5: Blue와 Green 역할 교체 (다음 배포 대비)**
```bash
# 다음 배포 시에는 Blue를 다시 시작하고 Green을 종료
# Green → 새 Blue, Blue → 새 Green
```

---

## 🔧 주요 명령어

### 컨테이너 관리
```bash
# 모든 컨테이너 상태 확인
docker-compose -f docker-compose.prod.yml ps

# 특정 컨테이너 재시작
docker-compose -f docker-compose.prod.yml restart mechuragi-main-blue

# 특정 컨테이너 로그
docker-compose -f docker-compose.prod.yml logs -f mysql
docker-compose -f docker-compose.prod.yml logs -f redis
docker-compose -f docker-compose.prod.yml logs -f mechuragi-main-blue

# 컨테이너 내부 접속
docker exec -it mechuragi-mysql mysql -u root -p<MYSQL_ROOT_PASSWORD>
docker exec -it mechuragi-redis redis-cli
docker exec -it mechuragi-main-blue bash
```

### 데이터 관리
```bash
# MySQL 데이터 백업
docker exec mechuragi-mysql mysqldump -u root -p<MYSQL_ROOT_PASSWORD> mechuragi_db > backup_$(date +%Y%m%d).sql

# MySQL 데이터 복원
docker exec -i mechuragi-mysql mysql -u root -p<MYSQL_ROOT_PASSWORD> mechuragi_db < backup.sql

# Redis 데이터 확인
docker exec mechuragi-redis redis-cli DBSIZE
docker exec mechuragi-redis redis-cli KEYS '*'

# 볼륨 확인
docker volume ls | grep mechuragi
```

### 이미지 관리
```bash
# 최신 이미지 가져오기
docker pull <DOCKERHUB_USERNAME>/mechuragi-app:latest

# 이미지 빌드 (로컬에서)
docker build -t <DOCKERHUB_USERNAME>/mechuragi-app:latest .

# 이미지 푸시 (Docker Hub)
docker push <DOCKERHUB_USERNAME>/mechuragi-app:latest

# 사용하지 않는 이미지 정리
docker image prune -a
```

---

## 🌐 네트워크 구성

### 포트 매핑

| 서비스 | 컨테이너 포트 | 호스트 포트 | 외부 접근 |
|--------|--------------|-----------|----------|
| MySQL | 3306 | 3306 | AI 서버 (<AI_SERVER_IP>) |
| Redis | 6379 | - | 내부만 (컨테이너 간) |
| Spring Boot Blue | 8080 | 8080 | Nginx Gateway |
| Spring Boot Green | 8080 | 8081 | Nginx Gateway |

### 컨테이너 간 통신

**통합 메인 서버 내부**:
- **MySQL ↔ Spring Boot**: `mysql:3306` (컨테이너 이름)
- **Redis ↔ Spring Boot**: `redis:6379` (컨테이너 이름)

**서버 간 통신**:
- **AI 서버 → MySQL**: `<MAIN_SERVER_IP>:3306` (Private IP)
- **Nginx → Spring Boot**: `<MAIN_SERVER_IP>:8080` 또는 `<MAIN_SERVER_IP>:8081`

---

## 🔐 환경변수

### .env (로컬 개발용)
```bash
DB_HOST=localhost
DB_PORT=3306
MYSQL_ROOT_PASSWORD=<your-local-password>
REDIS_HOST=localhost
REDIS_PORT=6379
JWT_SECRET=<your-jwt-secret>
KAKAO_CLIENT_ID=<your-kakao-client-id>
KAKAO_CLIENT_SECRET=<your-kakao-client-secret>
KAKAO_REDIRECT_URI=http://localhost:8080/login/oauth2/code/kakao
OAUTH2_REDIRECT_URI=http://localhost:3000/oauth2/callback
# ... 기타 환경변수
```

### .env.dev (프로덕션 배포용)
```bash
# 데이터베이스
DB_NAME=mechuragi_db
DB_USERNAME=<db-username>
DB_PASSWORD=<db-password>
MYSQL_ROOT_PASSWORD=<mysql-root-password>

# JWT
JWT_SECRET=<your-jwt-secret-key>

# AWS
AWS_REGION=ap-northeast-2
S3_BUCKET=<your-s3-bucket>
SES_FROM_EMAIL=<your-email@domain.com>
SUPPORT_EMAIL=<support-email@domain.com>

# OAuth2 (Kakao)
KAKAO_CLIENT_ID=<your-kakao-client-id>
KAKAO_CLIENT_SECRET=<your-kakao-client-secret>
KAKAO_REDIRECT_URI=https://your-domain.com/login/oauth2/code/kakao
OAUTH2_REDIRECT_URI=https://your-domain.com/oauth2/callback

# AI Service
AI_SERVICE_URL=http://<AI_SERVER_IP>:8082

# Docker
DOCKERHUB_USERNAME=<your-dockerhub-username>
```

---

## 🐛 트러블슈팅

### MySQL 연결 오류
```bash
# MySQL 헬스체크 확인
docker exec mechuragi-mysql mysqladmin ping -h localhost -u root -p<MYSQL_ROOT_PASSWORD>

# MySQL 로그 확인
docker-compose -f docker-compose.prod.yml logs mysql

# MySQL 내부 접속하여 확인
docker exec -it mechuragi-mysql mysql -u root -p<MYSQL_ROOT_PASSWORD>
mysql> SHOW DATABASES;
mysql> USE mechuragi_db;
mysql> SHOW TABLES;

# 컨테이너 재시작
docker-compose -f docker-compose.prod.yml restart mysql
```

### Redis 연결 오류
```bash
# Redis 연결 테스트
docker exec mechuragi-redis redis-cli ping

# Redis 정보 확인
docker exec mechuragi-redis redis-cli INFO

# Redis 로그 확인
docker-compose -f docker-compose.prod.yml logs redis
```

### Spring Boot 시작 실패
```bash
# 로그 확인 (가장 중요)
docker-compose -f docker-compose.prod.yml logs mechuragi-main-blue

# 헬스체크 확인
curl http://localhost:8080/actuator/health

# 환경변수 확인
docker exec mechuragi-main-blue env | grep SPRING

# 컨테이너 재시작
docker-compose -f docker-compose.prod.yml restart mechuragi-main-blue
```

### 포트 충돌
```bash
# 포트 사용 확인
sudo netstat -tulpn | grep :8080
sudo netstat -tulpn | grep :3306

# 기존 컨테이너 정리
docker-compose -f docker-compose.prod.yml down

# 완전 정리 (볼륨 포함)
docker-compose -f docker-compose.prod.yml down -v
```

### AI 서버에서 MySQL 접근 불가
```bash
# 통합 메인 서버에서 MySQL 포트 확인
sudo netstat -tulpn | grep 3306

# AI 서버에서 연결 테스트
ssh -J ubuntu@<GATEWAY_IP> ubuntu@<AI_SERVER_IP>
telnet <MAIN_SERVER_IP> 3306
```

---

## 📊 모니터링

### 리소스 사용량
```bash
# 전체 컨테이너 리소스
docker stats

# CPU/메모리 사용량 (실시간)
docker stats mechuragi-mysql mechuragi-redis mechuragi-main-blue

# 디스크 사용량
docker system df
```

### 로그 확인
```bash
# 실시간 로그 (모든 컨테이너)
docker-compose -f docker-compose.prod.yml logs -f

# 최근 100줄
docker-compose -f docker-compose.prod.yml logs --tail=100

# 특정 시간 이후 로그
docker-compose -f docker-compose.prod.yml logs --since 30m

# 로그 파일 위치 확인
docker inspect mechuragi-main-blue | grep LogPath
```

### 헬스체크
```bash
# Spring Boot 헬스체크
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health

# MySQL 헬스체크
docker exec mechuragi-mysql mysqladmin ping -h localhost -u root -p<MYSQL_ROOT_PASSWORD>

# Redis 헬스체크
docker exec mechuragi-redis redis-cli ping
```

---

## 🔄 마이그레이션 (기존 구조 → 새 구조)

### 1. 데이터 백업 (기존 서버)
```bash
# 기존 MySQL 서버에서 백업
ssh ubuntu@<OLD_MYSQL_SERVER_IP>
mysqldump -u mechuragi_user -p mechuragi_db > backup_$(date +%Y%m%d).sql

# 백업 파일을 로컬로 다운로드
scp ubuntu@<OLD_MYSQL_SERVER_IP>:~/backup_*.sql .

# 통합 메인 서버로 업로드
scp backup_*.sql ubuntu@<MAIN_SERVER_IP>:~/
```

### 2. 새 인프라 배포 (통합 메인 서버)
```bash
# 통합 메인 서버 접속
ssh -J ubuntu@<GATEWAY_IP> ubuntu@<MAIN_SERVER_IP>

# 프로젝트 클론 또는 파일 업로드
git clone https://github.com/your-repo/mechuragi_server.git
cd mechuragi_server

# .env.dev 파일 설정 확인
cat .env.dev

# docker-compose 실행
docker-compose -f docker-compose.prod.yml up -d

# 컨테이너 상태 확인
docker-compose -f docker-compose.prod.yml ps
```

### 3. 데이터 복원
```bash
# MySQL 컨테이너로 데이터 복원
docker exec -i mechuragi-mysql mysql -u <DB_USERNAME> -p<DB_PASSWORD> mechuragi_db < backup_*.sql

# 복원 확인
docker exec -it mechuragi-mysql mysql -u <DB_USERNAME> -p<DB_PASSWORD> mechuragi_db
mysql> SHOW TABLES;
mysql> SELECT COUNT(*) FROM users;
```

### 4. 검증
```bash
# API 헬스체크
curl http://localhost:8080/actuator/health

# MySQL 연결 테스트
docker exec mechuragi-mysql mysql -u <DB_USERNAME> -p<DB_PASSWORD> -e "SELECT COUNT(*) FROM mechuragi_db.users;"

# Redis 연결 테스트
docker exec mechuragi-redis redis-cli ping

# AI 서버에서 MySQL 접근 테스트
ssh -J ubuntu@<GATEWAY_IP> ubuntu@<AI_SERVER_IP>
mysql -h <MAIN_SERVER_IP> -u <DB_USERNAME> -p<DB_PASSWORD> mechuragi_db -e "SELECT 1;"
```

### 5. Nginx 설정 업데이트
```bash
# Nginx Gateway 서버에서
ssh ubuntu@<GATEWAY_IP>

# upstream을 새 서버로 변경
sudo vi /etc/nginx/conf.d/proxy.conf
# <OLD_API_SERVER_IP>:8080 → <MAIN_SERVER_IP>:8080

# Nginx 리로드
sudo nginx -t
sudo nginx -s reload
```

---

## 📚 참고 자료

- [Docker Compose 공식 문서](https://docs.docker.com/compose/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [MySQL Docker 이미지](https://hub.docker.com/_/mysql)
- [Redis Docker 이미지](https://hub.docker.com/_/redis)
- [메추라기 인프라 프로젝트](https://github.com/teamMechuragi/mechuragi_infra)

---

## 🎯 Quick Reference

### 로컬 개발
```bash
docker-compose up -d                    # 시작
docker-compose logs -f                  # 로그
docker-compose down                     # 중지
```

### 프로덕션 배포
```bash
# 서버 접속
ssh -J ubuntu@<GATEWAY_IP> ubuntu@<MAIN_SERVER_IP>

# 일반 배포
docker-compose -f docker-compose.prod.yml up -d

# Blue-Green 배포
docker-compose -f docker-compose.prod.yml --profile deploy up -d mechuragi-main-green
```

### 주요 포트
- **8080**: Spring Boot Blue
- **8081**: Spring Boot Green
- **3306**: MySQL (AI 서버 접근 가능)
- **6379**: Redis (내부만)

---

**최종 업데이트**: 2025-12-20
**작성자**: Claude Code (AI Assistant)
