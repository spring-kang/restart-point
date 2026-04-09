import { test, expect } from '@playwright/test';

test('비로그인 홈 화면의 핵심 CTA가 보인다', async ({ page }) => {
  await page.goto('/');

  await expect(page.getByRole('heading', { name: '부트캠프 수료 이후,' })).toBeVisible();
  await expect(page.getByRole('link', { name: '시작하기', exact: true })).toBeVisible();
  await expect(page.getByRole('link', { name: '시즌 둘러보기' })).toBeVisible();
  await expect(page.getByRole('link', { name: '무료로 시작하기' })).toBeVisible();
  await expect(page.getByRole('link', { name: '로그인' })).toBeVisible();
  await expect(page.getByRole('link', { name: '회원가입' })).toBeVisible();
});

