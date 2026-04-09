import { test, expect } from '@playwright/test';

test('로그인 페이지 기본 요소가 렌더링된다', async ({ page }) => {
  await page.goto('/login');

  await expect(page.getByRole('heading', { name: '로그인' })).toBeVisible();
  await expect(page.getByLabel('이메일')).toBeVisible();
  await expect(page.getByLabel('비밀번호')).toBeVisible();
  await expect(page.getByRole('button', { name: '로그인' })).toBeVisible();
});

test('존재하지 않는 이메일 로그인 시 에러가 노출된다', async ({ page }) => {
  await page.route('**/api/v1/auth/login', async (route) => {
    await route.fulfill({
      status: 404,
      contentType: 'application/json',
      body: JSON.stringify({
        errorCode: 'USER_001',
        message: '등록되지 않은 이메일입니다.',
      }),
    });
  });

  await page.goto('/login');
  await page.getByLabel('이메일').fill('nobody@example.com');
  await page.getByLabel('비밀번호').fill('wrong-password');
  await page.getByRole('button', { name: '로그인' }).click();

  await expect(page.getByText('등록되지 않은 이메일입니다.')).toBeVisible();
  await expect(page.getByText('회원가입을 진행해주세요.')).toBeVisible();
  await expect(page.getByRole('link', { name: '회원가입 하러 가기 →' })).toBeVisible();
});
