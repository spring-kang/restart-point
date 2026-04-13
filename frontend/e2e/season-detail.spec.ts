import { test, expect } from '@playwright/test';

const uncertifiedUser = {
  id: 1,
  email: 'user@restart-point.com',
  name: '미인증유저',
  role: 'USER',
  emailVerified: true,
  certificationStatus: 'NONE',
  createdAt: '2026-04-11T00:00:00Z',
  updatedAt: '2026-04-11T00:00:00Z',
};

const certifiedUser = {
  ...uncertifiedUser,
  email: 'approved@restart-point.com',
  name: '인증유저',
  certificationStatus: 'APPROVED',
};

const season = {
  id: 1,
  title: '테스트 시즌',
  description: '시즌 상세 E2E 테스트용 설명입니다.',
  status: 'RECRUITING',
  recruitmentStartAt: '2026-04-01T00:00:00',
  recruitmentEndAt: '2026-04-10T00:00:00',
  teamBuildingStartAt: '2026-04-11T00:00:00',
  teamBuildingEndAt: '2026-04-15T00:00:00',
  projectStartAt: '2026-04-16T00:00:00',
  projectEndAt: '2026-04-25T00:00:00',
  reviewStartAt: '2026-04-26T00:00:00',
  reviewEndAt: '2026-04-30T00:00:00',
  expertReviewWeight: 100,
  candidateReviewWeight: 0,
  createdAt: '2026-04-01T00:00:00Z',
  updatedAt: '2026-04-01T00:00:00Z',
  currentPhase: '참여자 모집 중',
  canJoin: true,
} as const;

const createAuthStorage = (user: typeof uncertifiedUser | typeof certifiedUser | null) => ({
  state: {
    user,
    accessToken: user ? 'fake-token' : null,
    isAuthenticated: Boolean(user),
  },
  version: 0,
});

test('존재하는 시즌 상세 진입 시 핵심 정보와 진행 중 타임라인이 보인다', async ({ page }) => {
  await page.addInitScript((storageValue) => {
    window.localStorage.setItem('auth-storage', JSON.stringify(storageValue));
    window.localStorage.removeItem('accessToken');
  }, createAuthStorage(null));

  await page.route('**/api/v1/seasons/1', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: season }),
    });
  });

  await page.goto('/seasons/1');

  await expect(page.getByRole('heading', { name: '테스트 시즌' })).toBeVisible();
  await expect(page.getByText('시즌 상세 E2E 테스트용 설명입니다.')).toBeVisible();
  await expect(page.getByText('참여자 모집 중')).toBeVisible();
  await expect(page.getByText('70%')).toBeVisible();
  await expect(page.getByText('30%')).toBeVisible();
  await expect(page.getByText('모집 기간')).toBeVisible();
  await expect(page.getByText('진행 중')).toBeVisible();
});

test('존재하지 않는 시즌 ID 접근 시 오류 안내와 목록 이동 CTA가 보인다', async ({ page }) => {
  await page.addInitScript((storageValue) => {
    window.localStorage.setItem('auth-storage', JSON.stringify(storageValue));
    window.localStorage.removeItem('accessToken');
  }, createAuthStorage(null));

  await page.route('**/api/v1/seasons/999', async (route) => {
    await route.fulfill({ status: 404, contentType: 'application/json', body: JSON.stringify({ success: false, message: '시즌을 찾을 수 없습니다.' }) });
  });

  await page.goto('/seasons/999');

  await expect(page.getByText('시즌 정보를 불러오는데 실패했습니다.')).toBeVisible();
  await expect(page.getByRole('link', { name: '시즌 목록으로' })).toBeVisible();
});

test('비로그인 사용자가 참여 가능한 시즌에 접근하면 로그인 필요 안내가 보인다', async ({ page }) => {
  await page.addInitScript((storageValue) => {
    window.localStorage.setItem('auth-storage', JSON.stringify(storageValue));
    window.localStorage.removeItem('accessToken');
  }, createAuthStorage(null));

  await page.route('**/api/v1/seasons/1', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: season }),
    });
  });

  await page.goto('/seasons/1');

  await expect(page.getByText('이 시즌에 참여하려면')).toBeVisible();
  await expect(page.getByRole('main').getByRole('link', { name: '로그인' })).toBeVisible();
  await expect(page.getByRole('link', { name: '팀 찾기 / 팀 만들기' })).not.toBeVisible();
});

test('로그인한 미인증 사용자가 접근하면 수료 인증 필요 안내가 보인다', async ({ page }) => {
  await page.addInitScript((storageValue) => {
    window.localStorage.setItem('accessToken', 'fake-token');
    window.localStorage.setItem('auth-storage', JSON.stringify(storageValue));
  }, createAuthStorage(uncertifiedUser));

  await page.route('**/api/v1/seasons/1', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: season }),
    });
  });

  await page.route('**/api/v1/notifications/unread-count', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: { count: 0 } }) });
  });

  await page.goto('/seasons/1');

  await expect(page.getByText('이 시즌에 참여하려면')).toBeVisible();
  await expect(page.getByRole('link', { name: '수료 인증' })).toBeVisible();
  await expect(page.getByRole('link', { name: '팀 찾기 / 팀 만들기' })).not.toBeVisible();
});

test('로그인한 인증 완료 사용자가 접근하면 팀 찾기 / 팀 만들기 버튼이 보인다', async ({ page }) => {
  await page.addInitScript((storageValue) => {
    window.localStorage.setItem('accessToken', 'fake-token');
    window.localStorage.setItem('auth-storage', JSON.stringify(storageValue));
  }, createAuthStorage(certifiedUser));

  await page.route('**/api/v1/seasons/1', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: season }),
    });
  });

  await page.route('**/api/v1/notifications/unread-count', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: { count: 0 } }) });
  });

  await page.goto('/seasons/1');

  await expect(page.getByRole('link', { name: '팀 찾기 / 팀 만들기' })).toBeVisible();
  await expect(page.getByText('이 시즌에 참여하려면')).not.toBeVisible();
});
