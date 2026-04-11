import { test, expect } from '@playwright/test';

const unauthenticatedState = {
  state: {
    user: null,
    accessToken: null,
    isAuthenticated: false,
  },
  version: 0,
};

const pendingUser = {
  id: 1,
  email: 'pending@restart-point.com',
  name: '대기유저',
  role: 'USER',
  emailVerified: true,
  certificationStatus: 'PENDING',
  bootcampName: '코드잇 스프린트',
  bootcampGeneration: '5기',
  createdAt: '2026-04-11T00:00:00Z',
  updatedAt: '2026-04-11T00:00:00Z',
};

const approvedUser = {
  ...pendingUser,
  email: 'approved@restart-point.com',
  name: '승인유저',
  certificationStatus: 'APPROVED',
};

const uncertifiedUser = {
  ...pendingUser,
  email: 'user@restart-point.com',
  name: '미인증유저',
  certificationStatus: 'NONE',
  bootcampName: undefined,
  bootcampGeneration: undefined,
};

const createAuthStorage = (user: typeof pendingUser | typeof approvedUser | typeof uncertifiedUser) => ({
  state: {
    user,
    accessToken: 'fake-token',
    isAuthenticated: true,
  },
  version: 0,
});

test('비로그인 사용자는 로그인 페이지로 이동한다', async ({ page }) => {
  await page.addInitScript((storageValue) => {
    window.localStorage.setItem('auth-storage', JSON.stringify(storageValue));
    window.localStorage.removeItem('accessToken');
  }, unauthenticatedState);

  await page.goto('/certification');

  await expect(page).toHaveURL(/\/login$/);
  await expect(page.getByRole('heading', { name: '로그인' })).toBeVisible();
});

test('인증 대기 상태면 대기 안내 화면이 보인다', async ({ page }) => {
  await page.addInitScript((storageValue) => {
    window.localStorage.setItem('accessToken', 'fake-token');
    window.localStorage.setItem('auth-storage', JSON.stringify(storageValue));
  }, createAuthStorage(pendingUser));

  await page.route('**/api/v1/users/me', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: pendingUser }),
    });
  });

  await page.goto('/certification');

  await expect(page.getByRole('heading', { name: '수료 인증' })).toBeVisible();
  await expect(page.getByText('인증 대기 중')).toBeVisible();
  await expect(page.getByText('운영자가 수료 인증을 검토 중입니다. 잠시만 기다려주세요.')).toBeVisible();
  await expect(page.getByRole('button', { name: '상태 새로고침' })).toBeVisible();
});

test('인증 완료 상태면 완료 안내 화면이 보인다', async ({ page }) => {
  await page.addInitScript((storageValue) => {
    window.localStorage.setItem('accessToken', 'fake-token');
    window.localStorage.setItem('auth-storage', JSON.stringify(storageValue));
  }, createAuthStorage(approvedUser));

  await page.route('**/api/v1/users/me', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: approvedUser }),
    });
  });

  await page.goto('/certification');

  await expect(page.getByText('인증 완료')).toBeVisible();
  await expect(page.getByText('코드잇 스프린트 5기 수료가 확인되었습니다.')).toBeVisible();
  await expect(page.getByRole('button', { name: '홈으로 돌아가기' })).toBeVisible();
});

test('수료 인증 요청 성공 시 홈으로 이동하고 인증대기 상태가 반영된다', async ({ page }) => {
  const requestedUser = {
    ...uncertifiedUser,
    certificationStatus: 'PENDING',
    bootcampName: '코드잇 스프린트',
    bootcampGeneration: '5기',
  };

  await page.addInitScript((storageValue) => {
    window.localStorage.setItem('accessToken', 'fake-token');
    window.localStorage.setItem('auth-storage', JSON.stringify(storageValue));
  }, createAuthStorage(uncertifiedUser));

  await page.route('**/api/v1/users/me', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: uncertifiedUser }),
    });
  });

  await page.route('**/api/v1/files/upload', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ url: 'https://files.example.com/certificate.png' }),
    });
  });

  await page.route('**/api/v1/users/me/certification', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: requestedUser }),
    });
  });

  await page.route('**/api/v1/notifications/unread-count', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: { count: 0 } }),
    });
  });

  await page.goto('/certification');

  await page.getByLabel('부트캠프명').fill('코드잇 스프린트');
  await page.getByLabel('기수').fill('5기');
  await page.getByLabel('수료일').fill('2026-03-31');
  await page.locator('input[type="file"]').setInputFiles({
    name: 'certificate.png',
    mimeType: 'image/png',
    buffer: Buffer.from('fake-image'),
  });

  await expect(page.getByText('certificate.png')).toBeVisible();
  await page.getByRole('button', { name: '수료 인증 요청' }).click();

  await expect(page).toHaveURL(/\/$/);
  await expect(page.getByText('미인증유저')).toBeVisible();
  await expect(page.getByText('인증대기')).toBeVisible();
});
