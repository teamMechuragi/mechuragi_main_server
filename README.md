# 🐦 메추라기
AI 기반 메뉴/식당 추천 커뮤니티 플랫폼

---

## 📱 주요 화면

### 🎨 스플래시 & 온보딩
<table>
  <tr>
    <td align="center">
      <img src="./assets/스플레시.png" width="250"/><br/>
      <i>"AI가 만드는 모두의 식탁" - 메추라기 시작 화면</i>
    </td>
    <td align="center">
      <img src="./assets/온보딩4.png" width="250"/><br/>
      <i>온보딩 화면</i>
    </td>
  </tr>
</table>

---

## 📌 프로젝트 소개

**Mechuragi**는 **Claude API**와 **Custom ML 모델**을 활용한 **AI 기반 메뉴/식당 추천 커뮤니티 플랫폼**입니다.

**🎯 핵심 기능**
1. **메뉴 추천 (Claude API)**
   - 사용자 취향 정보 (식사인원, 알러지, 다이어트 여부, 비건, 매운맛 정도, 선호 음식 종류, 선호 맛, 안 먹는 음식 등)
   - 오늘의 상황 정보 (날씨, 시간대, 기분, 재료 등)
   - → **Claude API 분석**을 통한 맞춤 메뉴 추천

2. **식당 추천 (Custom ML Model)**
   - 추천받은 메뉴 정보
   - 사용자 위치 정보
   - 식당 요구사항 (주차 가능 여부, 유아 동반 가능, 펫프렌들리, 비건, 할랄식당)
   - → **Custom ML 모델 분석**을 통한 최적 식당 추천

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
- **AI 장소 서버 (EC2)**: FastAPI 기반 식당 추천 ML 모델 (Sentence-BERT)
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

## 🎯 1단계: AI 메뉴 추천 (Claude API)

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

## 🏪 2단계: 맞춤 식당 추천 (Custom ML Model)

### 🗺️ 배리어프리 식당 필터링
<table>
  <tr>
    <td align="center" width="50%">
      <img src="./assets/장소 조건 설정.png" width="250"/><br/>
      <b>식당 요구사항 설정</b>
    </td>
    <td align="center" width="50%">
      <img src="./assets/장소 추천.png" width="250"/><br/>
      <b>AI 메뉴/장소 추천</b>
    </td>
  </tr>
</table>

**포용적 식문화를 위한 배리어프리 필터:**
- 🚗 **주차 가능** - 차량 이용자를 위한 주차 공간
- 🐾 **펫 프렌들리** - 반려동물 동반 가능
- ♿ **휠체어 접근** - 경사로, 승강기 등 이동약자 지원
- 👶 **유아 동반** - 유아 의자, 수유실 등 키즈 케어 시설
- 🥗 **비건/할랄** - 종교 및 채식 단계별 엄격한 식단 기준

**Sentence-BERT 기반 Custom ML 모델**이 사용자 위치, 취향, 배리어프리 조건을 종합하여 **최적의 식당**을 추천합니다. 특히 **30년 이상 전통 노포**에는 가중치를 부여하여 검증된 맛집을 우선 추천합니다.

---

## 🎯 제작 목표

🍽 Claude API 기반 맞춤 메뉴 추천
🏪 Custom ML 모델 기반 식당 추천
📅 먹방 일기 캘린더로 식사 기록
👥 커뮤니티 투표로 메뉴 선택
🔔 실시간 알림 및 인기메뉴 확인

---

## ✅ 기대 효과

- Claude API를 활용한 정확하고 맥락 있는 메뉴 추천
- Custom ML 모델을 통한 사용자 맞춤 식당 추천
- 상세한 식당 요구사항 반영 (주차, 유아동반, 펫프렌들리, 비건, 할랄)
- 메뉴 결정 스트레스 해소 및 식사 경험 향상
- 실시간 인기메뉴와 커뮤니티 기반 소통 경험 강화

---

## ⚙️ 주요 기능

### 🎯 핵심 추천 시스템

| 구분 | 기능 설명 |
|------|-----------|
| **메뉴 추천** | Claude API 기반 맞춤 메뉴 추천<br>- 사용자 취향 (식사인원, 알러지, 다이어트, 비건, 매운맛 정도, 선호 음식, 선호 맛, 안 먹는 음식)<br>- 상황 정보 (기분, 날씨, 재료, 시간대) |
| **식당 추천** | Custom ML 모델 기반 식당 추천<br>- 메뉴 정보 + 위치 정보<br>- 식당 요구사항 (주차 가능, 유아동반 가능, 펫프렌들리, 비건, 할랄식당) |

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
![React Native](https://img.shields.io/badge/React_Native-20232A?style=for-the-badge&logo=react&logoColor=61DAFB)
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

Spring Security 인증 오버헤드 측정을 통해 OpenResty Lua 기반 직접 경로의 지연 절감 효과 검증

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

| 이름 | 역할 | 담당 기능 |
|------|------|-----------|
| 🎨 박은진 | Design | - 서비스 전체 UI/UX 디자인 및 시각적 요소 기획 (Figma) |
| 🐰 김지영 | Frontend | - 프론트엔드 프로젝트 초기 설정 및 아키텍처 구성 <br> - 전체 페이지 구현 (온보딩 · 홈 · 로그인 · 회원가입 · 마이페이지 · 설정 · 캘린더 · 커뮤니티 · 메뉴 추천 전 유형) <br> - Header · Footer · Toast · NotificationBell 공통 컴포넌트 구현 <br> - Framer Motion 기반 페이지 전환 애니메이션 및 인터랙션 구현 <br> - 홈 화면 TMI 슬라이드 배너 기획 및 구현 (10종) <br> - 로딩 스피너 · 에러 상태 등 사용자 피드백 UI 처리 |
| 🦝 김진아 | Backend | - JWT 인증 · 카카오 OAuth2 소셜 로그인 · 이메일 인증 구현 <br> - AWS 인프라 구축 (Terraform: VPC · EC2 · S3 · CloudFront · ACM · SES · IAM) <br> - Ansible 플레이북 작성 및 서버 구성 자동화 <br> - Nginx 라우팅 · CloudFront 경로 설정 · 도메인 연동 <br> - CloudWatch 로그 그룹 설정 <br> - Redis Pub/Sub 기반 실시간 알림 · SSE 연동 · 인기 메뉴 스케줄러 구현 <br> - 프론트엔드 일부 API 연동 (AI 추천 · 취향 설정 · 북마크 · 알림 · 비밀번호 찾기 · 인기 메뉴) <br> - k6 부하 테스트 (AI 추천 경로 성능 비교) |
| 🐿️ 김희주 | Backend | - DB 설계 및 JPA 엔티티 모델링 <br> - 커뮤니티 투표 시스템 구현 (CRUD · 좋아요 · 댓글 · 핫한 투표 순위) <br> - 먹방 일기 구현 <br> - AWS Bedrock Claude API 연동 및 AI 메뉴 추천 서비스 구현 <br> - OpenResty + Lua JWT 인증 게이트웨이 구현 <br> - GitHub Actions + Docker 기반 Blue-Green 무중단 배포 구축 <br> - 프론트엔드 일부 API 연동 (먹방 일기 · 투표 · 온보딩 · 이미지 업로드) <br> - k6 부하 테스트 (이미지 업로드 방식 성능 검증) |
