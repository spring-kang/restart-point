import { test, expect } from '@playwright/test';
import {
  ADMIN_BASE_URL,
  ADMIN_TEST_EMAIL,
  ADMIN_TEST_PASSWORD,
  USER_BASE_URL,
  USER_TEST_EMAIL,
  USER_TEST_PASSWORD,
  isProductionTarget,
} from './env';

test.describe('production smoke - user web', () => {
  test.skip(!isProductionTarget, 'production target only');

  test('홈, 시즌, 팀 공개 페이지가 정상 노출된다', async ({ page }) => {
    await page.goto(USER_BASE_URL);
    await expect(page.getByRole('heading', { name: '부트캠프 수료 이후,' })).toBeVisible();

    await page.goto(`${USER_BASE_URL}/seasons`);
    await expect(page.getByRole('heading', { name: '시즌 공모전' })).toBeVisible();

    await page.goto(`${USER_BASE_URL}/teams`);
    await expect(page.getByRole('heading', { name: '팀 찾기' })).toBeVisible();
  });

  test('테스트 사용자 로그인 후 주요 사용자 페이지가 열린다', async ({ page }) => {
    await page.goto(`${USER_BASE_URL}/login`);
    await page.getByLabel('이메일').fill(USER_TEST_EMAIL);
    await page.getByLabel('비밀번호').fill(USER_TEST_PASSWORD);
    await page.getByRole('button', { name: '로그인' }).click();

    try {
      await expect(page).toHaveURL(/restart-point\.com\/?$/);
    } catch {
      const errorMessages = await page.locator('.text-red-500, .bg-red-50, [role="alert"]').allTextContents();
      const currentUrl = page.url();
      throw new Error(`User production login did not redirect. url=${currentUrl} errors=${JSON.stringify(errorMessages)}`);
    }

    await expect(page.getByRole('link', { name: '내 팀' })).toBeVisible();

    await page.goto(`${USER_BASE_URL}/my-team`);
    await expect(page.getByRole('heading', { name: '내 팀' })).toBeVisible();

    await page.goto(`${USER_BASE_URL}/profile`);
    await expect(page.getByRole('heading', { name: /프로필/ })).toBeVisible();
  });
});

test.describe('production smoke - admin web', () => {
  test.skip(!isProductionTarget, 'production target only');
  test.skip(!ADMIN_TEST_PASSWORD, 'ADMIN_TEST_PASSWORD secret is required');

  test('관리자 로그인 후 핵심 관리 화면이 열린다', async ({ page }) => {
    await page.goto(`${ADMIN_BASE_URL}/login`);
    await page.getByLabel('이메일').fill(ADMIN_TEST_EMAIL);
    await page.getByLabel('비밀번호').fill(ADMIN_TEST_PASSWORD);
    await page.getByRole('button', { name: '로그인' }).click();

    await expect(page.getByRole('heading', { name: '대시보드' })).toBeVisible();

    await page.goto(`${ADMIN_BASE_URL}/users`);
    await expect(page.getByRole('heading', { name: '회원 관리' })).toBeVisible();

    await page.goto(`${ADMIN_BASE_URL}/seasons`);
    await expect(page.getByRole('heading', { name: '시즌 관리' })).toBeVisible();

    await page.goto(`${ADMIN_BASE_URL}/certifications`);
    await expect(page.getByRole('heading', { name: '수료 인증 관리' })).toBeVisible();
  });
});
