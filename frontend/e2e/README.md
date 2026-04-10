# Playwright E2E

Playwright 기반 E2E 테스트 모음.

## 포함 범위
- 로컬 스모크 테스트
  - 홈 화면 스모크 테스트
  - 로그인 화면 렌더링 테스트
  - 로그인 에러 처리 테스트(mock API)
- 프로덕션 스모크 테스트
  - 사용자 웹 공개 페이지 확인
  - 사용자 테스트 계정 로그인 후 핵심 페이지 확인
  - 관리자 로그인 후 핵심 관리 페이지 확인

## 로컬 실행
```bash
cd frontend
npm install
npx playwright install
npm run test:e2e
```

## 프로덕션 실행
```bash
cd frontend
$env:E2E_TARGET="production"
$env:USER_BASE_URL="https://restart-point.com"
$env:ADMIN_BASE_URL="https://admin.restart-point.com"
$env:USER_TEST_EMAIL="test1@restart-point.com"
$env:USER_TEST_PASSWORD="1234"
$env:ADMIN_TEST_EMAIL="admin@restart-point.com"
$env:ADMIN_TEST_PASSWORD="<admin-password>"
npm run test:e2e -- production.smoke.spec.ts
```

## GitHub Actions hourly smoke
`.github/workflows/production-e2e.yml` 에서 매시간 운영 스모크 테스트를 실행한다.

필수 GitHub Secrets:
- `USER_TEST_EMAIL`
- `USER_TEST_PASSWORD`
- `ADMIN_TEST_EMAIL`
- `ADMIN_TEST_PASSWORD`

선택 GitHub Secret:
- `DISCORD_E2E_WEBHOOK_URL`
  - 설정하면 매시간 실행 결과를 Discord로 전송한다.
  - 성공/실패 여부, 실행 링크, artifact 링크를 함께 보낸다.

## 리뷰 E2E용 데이터 시드
리뷰 플로우를 실제 API 기준으로 테스트하려면 아래 조건이 필요하다.
- `테스트 시즌` 이 `REVIEWING` 상태일 것
- 로그인 가능한 리뷰어 계정이 있을 것
- 심사 대상이 되는 `SUBMITTED` 프로젝트가 있을 것
- 필요 시 `내가 심사한 목록` 확인용 기존 리뷰 이력이 있을 것

실행용 PostgreSQL SQL:
- `scripts/e2e-review-seed.sql`
- cleanup: `scripts/e2e-review-cleanup.sql`

기본 계정
- `test1@restart-point.com / 1234` → 리뷰어 계정
- `test2@restart-point.com / 1234` → 심사 대상 팀장 계정

## 다음 추천 작업
1. 테스트 시즌(`테스트 시즌`) 전용 write 시나리오 설계
2. 회원가입/팀 생성/팀 지원/관리자 승인 테스트 분리
3. 운영 데이터 cleanup 규칙 추가
4. 실패 시 Discord/Slack 알림 연동
