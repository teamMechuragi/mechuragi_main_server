# 🐦 메추라기
AI 기반 메뉴/식당 추천 커뮤니티 플랫폼

---

## 📱 주요 화면

### 🎬 앱 데모
<p align="center">
  <img src="./assets/demo.gif" width="300"/>
</p>

### 🎨 스플래시 & 온보딩
<table>
  <tr>
    <td align="center">
      <img src="./assets/스플레시.png" width="200"/><br/>
      <i>메추라기 시작 화면</i>
    </td>
    <td align="center">
      <img src="./assets/온보딩1.png" width="200"/><br/>
      <i>AI 메뉴 추천</i>
    </td>
    <td align="center">
      <img src="./assets/온보딩2.png" width="200"/><br/>
      <i>먹방 일기 캘린더</i>
    </td>
    <td align="center">
      <img src="./assets/온보딩3.png" width="200"/><br/>
      <i>커뮤니티 투표</i>
    </td>
    <td align="center">
      <img src="./assets/온보딩4.png" width="200"/><br/>
      <i>취향 설정</i>
    </td>
  </tr>
</table>

---

## 📌 프로젝트 소개

**Mechuragi**는 **Claude API**를 활용한 **AI 기반 메뉴 추천 커뮤니티 플랫폼**입니다.

**🎯 핵심 기능**
1. **메뉴 추천 (Claude API)**
   - 사용자 취향 정보 (식사인원, 알러지, 다이어트 여부, 비건, 매운맛 정도, 선호 음식 종류, 선호 맛, 안 먹는 음식 등)
   - 오늘의 상황 정보 (날씨, 시간대, 기분, 재료 등)
   - → **Claude API 분석**을 통한 맞춤 메뉴 추천

**추가 기능**: 실시간 인기메뉴, 투표 커뮤니티, 먹방 일기, 실시간 알림 (SSE)

---

## 🏗️ 시스템 아키텍처

<p align="center">
  <img src="./assets/시스템_아키텍처.png" width="800"/>
</p>

**Infrastructure as Code (IaC) 기반 자동화 배포**
- **Terraform (HCL)**: AWS 인프라 프로비저닝 (VPC, EC2, S3, CloudFront, ACM 등)
- **Ansible**: 서버 구성 관리 및 Docker 컨테이너 배포
- **GitHub Actions**: CI/CD 파이프라인 자동화 (빌드, 테스트, Docker 이미지 푸시)
- **Docker Hub**: 컨테이너 이미지 레지스트리

**Multi-Server Architecture**
- **메인 서버 (EC2)**: Spring Boot 기반 API 서버, SSE 실시간 알림, Redis 캐싱, MySQL DB
- **AI 추천 서버 (EC2)**: 메뉴 추천 AI 서비스 (AWS Bedrock Claude API 연동)
- **Nginx 서버 (EC2)**: OpenResty + Lua 기반 JWT 인증 게이트웨이, 리버스 프록시, NAT 인스턴스, 블루-그린 배포

**Frontend & CDN**
- **S3**: React 프론트엔드 정적 파일 및 이미지 저장소
- **CloudFront**: 글로벌 CDN으로 낮은 지연시간 제공, HTTPS 지원 (ACM 인증서)

---

## 📊 ERD

<p align="center">
  <img src="./assets/ERD.png" width="800"/>
</p>

---

## 🎯 AI 메뉴 추천 (Claude API)

### 📝 취향 설정
<p align="center">
  <img src="./assets/음식 취향 설정.png" width="250"/>
</p>

사용자의 식사 인원, 알레르기, 다이어트 여부, 비건 단계, 매운맛 선호도, 좋아하는 음식/싫어하는 음식 등 **상세한 취향 정보를 등록**합니다.

### 🎭 다양한 추천 방식

<table>
  <tr>
    <td align="center" width="33%">
      <img src="./assets/기분기반 메뉴추천.png" width="200"/><br/>
      <b>기분 기반 추천</b><br/>
      <i>오늘의 기분에 맞는 메뉴</i>
    </td>
    <td align="center" width="33%">
      <img src="./assets/날씨기반 메뉴추천.png" width="200"/><br/>
      <b>날씨 기반 추천</b><br/>
      <i>실시간 날씨를 고려한 메뉴</i>
    </td>
    <td align="center" width="33%">
      <img src="./assets/대화기반 메뉴추천.png" width="200"/><br/>
      <b>AI 대화 추천</b><br/>
      <i>자연어로 상황을 설명하면<br/>Claude가 분석</i>
    </td>
  </tr>
</table>

**Claude API**가 사용자의 취향 정보와 실시간 상황(기분, 날씨, 시간대, 재료)을 종합 분석하여 **개인 맞춤형 메뉴**를 추천합니다.

---

## 🎯 제작 목표

🍽 Claude API 기반 맞춤 메뉴 추천
📅 먹방 일기 캘린더로 식사 기록
👥 커뮤니티 투표로 메뉴 선택
🔔 실시간 알림 및 인기메뉴 확인

---

## ✅ 기대 효과

- Claude API를 활용한 정확하고 맥락 있는 메뉴 추천
- 메뉴 결정 스트레스 해소 및 식사 경험 향상
- 실시간 인기메뉴와 커뮤니티 기반 소통 경험 강화

---

## ⚙️ 주요 기능

### 🎯 핵심 추천 시스템

| 구분 | 기능 설명 |
|------|-----------|
| **메뉴 추천** | Claude API 기반 맞춤 메뉴 추천<br>- 사용자 취향 (식사인원, 알러지, 다이어트, 비건, 매운맛 정도, 선호 음식, 선호 맛, 안 먹는 음식)<br>- 상황 정보 (기분, 날씨, 재료, 시간대) |

### 📱 부가 기능

| 구분 | 기능 설명 |
|------|-----------|
| 실시간 인기메뉴 | 현재 가장 인기 있는 메뉴 실시간 확인 |
| 투표 커뮤니티 | "오늘 뭐 먹지?" 주제로 투표 생성 및 참여 |
| 먹방 일기 | 캘린더 기반의 식사 기록 및 사진 저장 |
| 실시간 알림 | 투표 결과, 추천 알림 등 실시간 알림 (SSE) |
| 사용자 기능 | 로그인 / 회원가입 / 설정 |

---

## 💻 기술 스택

### 🎨 Frontend
![React](https://img.shields.io/badge/React-61DAFB?style=for-the-badge&logo=React&logoColor=black)
![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?style=for-the-badge&logo=typescript&logoColor=white)
![Next.js](https://img.shields.io/badge/Next.js-000000?style=for-the-badge&logo=nextdotjs&logoColor=white)
![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white)

### 🛠 Backend
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![JPA](https://img.shields.io/badge/JPA-59666C?style=for-the-badge)
![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)
![OAuth](https://img.shields.io/badge/OAuth-EB5424?style=for-the-badge&logo=auth0&logoColor=white)
![SSE](https://img.shields.io/badge/SSE-E34F26?style=for-the-badge&logo=html5&logoColor=white)

### 🗄 Database & Cache
![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)

### ☁️ Infra
![AWS EC2](https://img.shields.io/badge/AWS_EC2-FF9900?style=for-the-badge&logo=amazonaws&logoColor=white)
![AWS S3](https://img.shields.io/badge/AWS_S3-569A31?style=for-the-badge&logo=amazons3&logoColor=white)
![CloudFront](https://img.shields.io/badge/CloudFront-232F3E?style=for-the-badge&logo=amazonaws&logoColor=white)
![AWS SES](https://img.shields.io/badge/AWS_SES-DD344C?style=for-the-badge&logo=amazonaws&logoColor=white)
![AWS Bedrock](https://img.shields.io/badge/AWS_Bedrock-FF9900?style=for-the-badge&logo=amazonaws&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![OpenResty](https://img.shields.io/badge/OpenResty-009639?style=for-the-badge&logo=nginx&logoColor=white)
![Lua](https://img.shields.io/badge/Lua-2C2D72?style=for-the-badge&logo=lua&logoColor=white)

### 🔄 CI/CD
![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)
![Terraform](https://img.shields.io/badge/Terraform-7B42BC?style=for-the-badge&logo=terraform&logoColor=white)
![Ansible](https://img.shields.io/badge/Ansible-EE0000?style=for-the-badge&logo=ansible&logoColor=white)
![HCL](https://img.shields.io/badge/HCL-7B42BC?style=for-the-badge&logo=terraform&logoColor=white)

### 📊 Logging
![CloudWatch](https://img.shields.io/badge/AWS_CloudWatch-FF9900?style=for-the-badge&logo=amazonaws&logoColor=white)

### ⚖️ Load Balancing & Testing
![NGINX](https://img.shields.io/badge/NGINX-009639?style=for-the-badge&logo=nginx&logoColor=white)
![k6](https://img.shields.io/badge/k6-7D64FF?style=for-the-badge&logo=k6&logoColor=white)

**이미지 업로드: Legacy vs Pre-signed URL**

| 엔드포인트 | Legacy (avg) | Pre-signed URL (avg) | 개선율 |
|------------|:------------:|:--------------------:|:------:|
| 프로필 이미지 | ~339ms | 122ms | **↓ 약 64%** |
| 다이어리 이미지 | ~117ms | 71ms | **↓ 약 39%** |
| 투표 이미지 (2장) | ~184ms | 160ms | **↓ 약 13%** |

서버가 이미지 바이트를 직접 처리하지 않아 고부하 시 서버 부담 감소 → Pre-signed URL 방식 채택

**AI 추천 경로: Direct vs Legacy**

| 경로 | 흐름 |
|------|------|
| **Direct** | Client → Nginx (Lua JWT 검증) → AI Server → Bedrock |
| **Legacy** | Client → Nginx → Main Server (Spring Security JWT 검증) → AI Server → Bedrock |

**테스트 환경**

| 항목 | 설정값 |
|------|--------|
| 도구 | k6 |
| 가상 사용자 (VU) | 50명 동시 요청 |
| 테스트 시간 | 60초 |
| Mock 지연 | 3,000ms (Bedrock) + 200ms (VPC 네트워크) = 3,200ms |
| Tomcat 스레드 제한 | Main Server 32개, AI Server 32개 |

> Bedrock 호출을 Mock으로 대체하여 외부 변수를 제거하고 인프라 구조 자체의 성능만 측정

**측정 결과**

| 지표 | Direct Path | Legacy Path | 차이 |
|------|:-----------:|:-----------:|:----:|
| avg | 3,231ms | 5,033ms | **+56%** |
| p95 | 3,545ms | 6,470ms | **+82%** |
| max | 3,556ms | 10,321ms | **+190%** |
| 처리량 (60s) | 285건 | 195건 | **-32%** |
| 에러율 | 0% | 0% | - |

**분석**

Legacy Path는 Main Server에서 1차 큐잉, AI Server에서 2차 큐잉이 직렬로 발생하는 구조적 문제가 확인되었습니다. Main Server가 RestClient(동기 블로킹)로 AI Server를 호출하는 구조상, Main Server 스레드는 AI Server 응답이 올 때까지 점유되어 부하 증가 시 두 서버의 스레드 풀이 동시에 소진됩니다.

Direct Path는 스레드 소비 지점이 1개로, 응답 시간이 Mock 지연(3,200ms)에 수렴하며 편차가 거의 없었습니다. Lua 인증 오버헤드(~1~5ms)는 구조적 병목과 비교해 무시 가능한 수준으로, **처리량 32% 향상, p95 응답시간 45% 단축** 효과를 확인하여 Direct Path를 채택하였습니다.

### 🔗 Version Control
![GitHub](https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=github&logoColor=white)

### 🎨 Design
![Figma](https://img.shields.io/badge/Figma-F24E1E?style=for-the-badge&logo=figma&logoColor=white)

---

## 🤝 협업

### 📋 회의록 정리
![Notion](https://img.shields.io/badge/Notion-000000?style=for-the-badge&logo=notion&logoColor=white)

> 📎 [Notion 프로젝트 페이지 바로가기](노션 링크 삽입)

### 💬 소통
![Discord](https://img.shields.io/badge/Discord-5865F2?style=for-the-badge&logo=discord&logoColor=white)

### 📎 협업 문서

| 문서 | 링크 |
|------|------|
| 📄 API 설계도 | [바로가기](./docs/API설계도.pdf) |
| 📊 WBS | [바로가기](./docs/WBS.pdf) |

---

## 👥 팀원 소개

| 이름 | 역할 | 담당 기능                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
|------|------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 🎨 박은진 | Design | - 서비스 전체 UI/UX 디자인 및 시각적 요소 기획 (Figma)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| 🐰 김지영 | Frontend | - 프론트엔드 프로젝트 초기 설정 및 아키텍처 구성 <br> - 전체 페이지 구현 (온보딩 · 홈 · 로그인 · 회원가입 · 마이페이지 · 설정 · 캘린더 · 커뮤니티 · 메뉴 추천 전 유형) <br> - Header · Footer · Toast · NotificationBell 공통 컴포넌트 구현 <br> - Framer Motion 기반 페이지 전환 애니메이션 및 인터랙션 구현 <br> - 홈 화면 TMI 슬라이드 배너 기획 및 구현 (10종) <br> - 로딩 스피너 · 에러 상태 등 사용자 피드백 UI 처리                                                                                                                                                                                                                                                                                                                                                                                          |
| 🦝 김진아 | Backend | - JWT 인증 (Access/Refresh Token 발급·갱신) · 카카오 OAuth2 소셜 로그인 · Spring Security 설정 <br> - 회원 도메인 구현 (회원가입 · 프로필 수정 · 비밀번호 변경 · 소프트 탈퇴 · 알림 설정) <br> - 이메일 인증 (AWS SES) · 비밀번호 찾기 · 환영 메일 발송 <br> - 음식 취향 설정 CRUD ([상세보기](./docs/trouble-shooting/preference-refactoring.md)) <br> - AI 추천 결과 저장/조회 · 북마크 기능 <br> - SSE 실시간 알림 (투표 마감 임박·종료) · Redis 기반 인기 메뉴 스케줄러 <br> - AWS 인프라 구축 (Terraform: VPC · EC2 · S3 · CloudFront · ACM · SES · IAM) <br> - Ansible 서버 구성 자동화 · Nginx 라우팅 · CloudWatch 로그 설정 <br> - 프론트엔드 일부 API 연동 (AI 추천 · 취향 설정 · 북마크 · 알림 · 비밀번호 찾기 · 인기 메뉴) <br> - k6 부하 테스트 (AI 추천 경로 성능 비교) <br> - AI 추천 서비스 전면 리팩토링 <br> - OpenResty + Lua JWT 인증 게이트웨이 전체 재구현 ([*관련문서 바로가기*](./docs/trouble-shooting/nginx-gateway-refactoring.md)) |
| 🐿️ 김희주 | Backend | - DB 설계 및 JPA 엔티티 모델링 <br> - 커뮤니티 투표 시스템 구현 (CRUD · Redis 기반 핫한 투표 순위 · 좋아요 · 댓글) <br> - 먹방 일기 구현 <br> - AWS Bedrock Claude API 연동 및 AI 메뉴 추천 서비스 구현 <br> - OpenResty + Lua JWT 인증 게이트웨이 구현 <br> - GitHub Actions + Docker 기반 Blue-Green 무중단 배포 구축 <br> - 프론트엔드 일부 API 연동 (먹방 일기 · 투표 · 온보딩 · 이미지 업로드) <br> - k6 부하 테스트 (이미지 업로드 방식 성능 검증)                                                                                                                                                                                                                                                                                                                                                            |
