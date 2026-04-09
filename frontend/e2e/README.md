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

## 다음 추천 작업
1. 테스트 시즌(`테스트 시즌`) 전용 write 시나리오 설계
2. 회원가입/팀 생성/팀 지원/관리자 승인 테스트 분리
3. 운영 데이터 cleanup 규칙 추가
4. 실패 시 Discord/Slack 알림 연동
