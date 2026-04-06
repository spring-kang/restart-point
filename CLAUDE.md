# CLAUDE.md

이 문서는 Claude Code가 Re:Start Point 프로젝트에서 작업할 때 참고하는 가이드라인입니다.

## 프로젝트 개요

**Re:Start Point**는 부트캠프 수료 이후의 성장을 다시 시작하는 AI 프로젝트 러닝 플랫폼입니다.

- 수료생 대상 프로젝트형 공모전 플랫폼
- AI 기반 팀 매칭, 프로젝트 코칭, 성장 리포트 제공
- 주요 문서: [PRD.md](./PRD.md)

## 기술 스택 (예정)

### Backend
- Java 17+
- Spring Boot 3.x
- Spring Security (인증/인가)
- Spring Data JPA
- PostgreSQL

### Frontend
- React 또는 Next.js
- TypeScript

### AI/ML
- OpenAI API 또는 Claude API
- 팀 매칭, 프로젝트 코칭, 성장 리포트 생성

### Infrastructure
- Docker
- AWS 또는 GCP

## 언어 및 커뮤니케이션 규칙

- **기본 응답 언어:** 한국어
- **코드 주석:** 한국어
- **커밋 메시지:** 한국어 (형식: `feat: 기능 설명` 또는 `fix: 수정 내용`)
- **문서화:** 한국어
- **변수명/함수명:** 영어 (코드 표준 준수)

## 코딩 컨벤션

### 일반
- 들여쓰기: 2칸 (프론트엔드), 4칸 (백엔드 Java)
- 파일 끝에 빈 줄 추가
- 불필요한 공백 제거

### Java/Spring
- Google Java Style Guide 기반
- 클래스명: PascalCase
- 메서드/변수명: camelCase
- 상수: UPPER_SNAKE_CASE
- 패키지명: 소문자

### 계층 구조
```
src/main/java/com/restartpoint/
├── domain/           # 도메인별 패키지
│   ├── user/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   └── dto/
│   ├── team/
│   ├── project/
│   ├── season/
│   └── review/
├── global/           # 공통 설정
│   ├── config/
│   ├── exception/
│   ├── security/
│   └── util/
└── infra/            # 외부 연동
    ├── ai/
    └── notification/
```

### TypeScript/React
- ESLint + Prettier 적용
- 컴포넌트: PascalCase
- 함수/변수: camelCase
- 상수: UPPER_SNAKE_CASE

## Git 브랜치 전략

```
main
├── develop
│   ├── feature/FR-01-user-auth
│   ├── feature/FR-02-profile
│   ├── feature/FR-03-season
│   └── ...
├── release/v1.0.0
└── hotfix/critical-bug
```

### 커밋 메시지 형식

```
<type>: <subject>

<body> (선택)

<footer> (선택)
```

**Type:**
- `feat`: 새로운 기능
- `fix`: 버그 수정
- `docs`: 문서 수정
- `style`: 코드 포맷팅
- `refactor`: 리팩토링
- `test`: 테스트 코드
- `chore`: 빌드, 설정 변경

**예시:**
```
feat: 수료 인증 API 구현

- 수료증 업로드 기능 추가
- 운영자 승인/반려 기능 추가

Refs: FR-01
```

## 주요 도메인 용어

| 한글 | 영문 | 설명 |
|------|------|------|
| 수료생 | Graduate | 부트캠프 수료 인증 완료 사용자 |
| 예비 참여자 | Candidate | 다음 기수 참여 예정자 |
| 시즌 | Season | 프로젝트 공모전 단위 |
| 체크포인트 | Checkpoint | 주차별 프로젝트 진행 기록 |
| 성장 리포트 | GrowthReport | AI 기반 개인/팀 피드백 |
| 루브릭 | Rubric | 심사 평가 기준 |

## API 설계 원칙

### RESTful 규칙
- 리소스 중심 URL 설계
- HTTP 메서드 의미에 맞게 사용
- 복수형 명사 사용

### URL 패턴 예시
```
GET    /api/v1/seasons                    # 시즌 목록
POST   /api/v1/seasons                    # 시즌 생성
GET    /api/v1/seasons/{seasonId}         # 시즌 상세
GET    /api/v1/seasons/{seasonId}/teams   # 시즌 내 팀 목록
POST   /api/v1/teams                      # 팀 생성
POST   /api/v1/teams/{teamId}/members     # 팀원 추가
GET    /api/v1/users/me/profile           # 내 프로필
```

### 응답 형식
```json
{
  "success": true,
  "data": { ... },
  "message": null
}
```

### 에러 응답
```json
{
  "success": false,
  "data": null,
  "message": "인증이 필요합니다.",
  "errorCode": "AUTH_001"
}
```

## 테스트 전략

- 단위 테스트: JUnit 5, Mockito
- 통합 테스트: @SpringBootTest
- API 테스트: MockMvc 또는 RestAssured
- 테스트 커버리지 목표: 70% 이상

## 보안 체크리스트

- [ ] SQL Injection 방지 (JPA 사용)
- [ ] XSS 방지
- [ ] CSRF 토큰 적용
- [ ] 인증/인가 검증
- [ ] 민감 정보 암호화 (수료증 등)
- [ ] Rate Limiting 적용
- [ ] CORS 설정

## 참고 문서

- [PRD.md](./PRD.md) - 제품 요구사항 문서
- [Spring Boot 공식 문서](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [JPA 가이드](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
