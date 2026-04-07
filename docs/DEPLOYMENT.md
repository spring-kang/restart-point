# 배포 가이드

Re:Start Point 프로젝트의 배포 설정 가이드입니다.

## 아키텍처

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│     Vercel      │────▶│     Railway     │────▶│    Railway      │
│   (Frontend)    │     │    (Backend)    │     │  (PostgreSQL)   │
│   React + Vite  │     │  Spring Boot    │     │                 │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

## 1. Railway 설정 (Backend + PostgreSQL)

### 1.1 Railway 프로젝트 생성

1. [Railway](https://railway.app) 접속 및 로그인
2. "New Project" → "Deploy from GitHub repo" 선택
3. `spring-kang/restart-point` 저장소 연결
4. Root Directory를 `backend`로 설정

### 1.2 PostgreSQL 추가

1. Railway 프로젝트 대시보드에서 "New" → "Database" → "PostgreSQL" 선택
2. 자동으로 `DATABASE_URL` 환경변수가 설정됨

### 1.3 환경변수 설정

Railway 프로젝트 Settings → Variables에서 다음 환경변수 추가:

```
# Database (PostgreSQL 연결 시 자동 설정됨)
DATABASE_URL=<자동 설정>

# JWT 설정 (필수 - 안전한 키로 변경)
JWT_SECRET=your-secure-jwt-secret-key-minimum-32-characters
JWT_EXPIRATION=86400000

# CORS (Vercel 배포 URL로 변경)
CORS_ALLOWED_ORIGINS=https://your-app.vercel.app

# Spring Profile
SPRING_PROFILES_ACTIVE=prod
```

### 1.4 Railway Token 발급 (GitHub Actions용)

1. Railway 대시보드 → Account Settings → Tokens
2. "New Token" 생성
3. GitHub Repository → Settings → Secrets → Actions
4. `RAILWAY_TOKEN` 이름으로 토큰 저장

## 2. Vercel 설정 (Frontend)

### 2.1 Vercel 프로젝트 생성

1. [Vercel](https://vercel.com) 접속 및 로그인
2. "Add New" → "Project" → GitHub 저장소 import
3. Root Directory를 `frontend`로 설정
4. Framework Preset: `Vite` 선택

### 2.2 환경변수 설정

Vercel 프로젝트 Settings → Environment Variables:

```
VITE_API_URL=https://your-railway-app.railway.app/api/v1
```

### 2.3 Vercel Token 발급 (GitHub Actions용)

1. Vercel 대시보드 → Settings → Tokens
2. "Create Token" 생성
3. GitHub Repository → Settings → Secrets → Actions
4. `VERCEL_TOKEN` 이름으로 토큰 저장

## 3. GitHub Secrets 설정

GitHub Repository → Settings → Secrets and variables → Actions에서 다음 시크릿 추가:

| Secret Name | 설명 |
|-------------|------|
| `RAILWAY_TOKEN` | Railway API 토큰 |
| `VERCEL_TOKEN` | Vercel API 토큰 |

## 4. 배포 흐름

### 자동 배포 (CI/CD)

1. `main` 브랜치에 push 또는 PR 머지
2. GitHub Actions 실행:
   - CI: Backend/Frontend 빌드 및 테스트
   - Deploy: Railway(Backend) + Vercel(Frontend) 배포

### 수동 배포

GitHub Actions → "Deploy" 워크플로우 → "Run workflow"

## 5. 로컬 개발 환경

### Backend

```bash
cd backend
./gradlew bootRun
# http://localhost:8080
```

### Frontend

```bash
cd frontend
npm install
npm run dev
# http://localhost:5173
```

## 6. 환경별 설정

| 환경 | Backend | Frontend | Database |
|------|---------|----------|----------|
| Local | `localhost:8080` | `localhost:5173` | H2 (in-memory) |
| Production | Railway | Vercel | PostgreSQL (Railway) |

## 7. 트러블슈팅

### CORS 에러

- Backend의 `CORS_ALLOWED_ORIGINS` 환경변수에 Frontend URL이 포함되어 있는지 확인
- 여러 도메인인 경우 쉼표로 구분: `https://app.vercel.app,https://custom-domain.com`

### Database 연결 실패

- Railway PostgreSQL이 정상 실행 중인지 확인
- `DATABASE_URL` 환경변수가 올바르게 설정되어 있는지 확인

### JWT 인증 오류

- Backend와 Frontend가 동일한 JWT_SECRET을 사용하는지 확인
- 토큰 만료 시간 확인

## 8. 모니터링

- **Railway**: 대시보드에서 로그 및 메트릭 확인
- **Vercel**: 대시보드에서 배포 로그 및 Analytics 확인
- **Health Check**: `https://your-railway-app.railway.app/actuator/health`
