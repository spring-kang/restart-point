import { test, expect } from '@playwright/test';

const signupUser = {
  id: 101,
  email: 'signup-e2e@restart-point.com',
  name: '회원가입테스트',
  role: 'USER',
  emailVerified: true,
  certificationStatus: 'NONE',
  createdAt: '2026-04-11T00:00:00Z',
  updatedAt: '2026-04-11T00:00:00Z',
};

test('이메일 인증 코드 발송 후 인증 단계로 이동한다', async ({ page }) => {
  await page.route('**/api/v1/auth/email/send', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: null, message: '인증 코드가 발송되었습니다.' }),
    });
  });

  await page.goto('/signup');
  await page.getByLabel('이메일').fill('signup-e2e@restart-point.com');
  await page.getByRole('button', { name: '인증 코드 받기' }).click();

  await expect(page.getByRole('heading', { name: '이메일 인증' })).toBeVisible();
  await expect(page.getByText('으로 발송된 6자리 인증 코드를 입력해주세요.')).toBeVisible();
});

test('6자리 미만 인증 코드는 에러가 노출된다', async ({ page }) => {
  await page.route('**/api/v1/auth/email/send', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: null }),
    });
  });

  await page.goto('/signup');
  await page.getByLabel('이메일').fill('signup-e2e@restart-point.com');
  await page.getByRole('button', { name: '인증 코드 받기' }).click();

  await page.locator('input[inputmode="numeric"]').nth(0).fill('1');
  await page.locator('input[inputmode="numeric"]').nth(1).fill('2');
  await page.locator('form').evaluate((form: HTMLFormElement) => form.requestSubmit());

  await expect(page.getByText('6자리 인증 코드를 입력해주세요.', { exact: true })).toBeVisible();
});

test('인증 성공 후 회원정보 입력 단계로 이동한다', async ({ page }) => {
  await page.route('**/api/v1/auth/email/send', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: null }),
    });
  });

  await page.route('**/api/v1/auth/email/verify', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: { signupToken: 'mock-signup-token' },
        message: '이메일 인증이 완료되었습니다. 회원가입을 진행해주세요.',
      }),
    });
  });

  await page.goto('/signup');
  await page.getByLabel('이메일').fill('signup-e2e@restart-point.com');
  await page.getByRole('button', { name: '인증 코드 받기' }).click();

  for (const [index, digit] of ['1', '2', '3', '4', '5', '6'].entries()) {
    await page.locator('input[inputmode="numeric"]').nth(index).fill(digit);
  }

  await page.getByRole('button', { name: '인증하기' }).click();

  await expect(page.getByRole('heading', { name: '이메일 인증 완료' })).toBeVisible();
  await expect(page.getByLabel('이름')).toBeVisible();
  await expect(page.getByLabel('비밀번호', { exact: true })).toBeVisible();
});

test('비밀번호 불일치 시 회원가입이 막힌다', async ({ page }) => {
  await page.route('**/api/v1/auth/email/send', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: null }) });
  });

  await page.route('**/api/v1/auth/email/verify', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: { signupToken: 'mock-signup-token' } }),
    });
  });

  await page.goto('/signup');
  await page.getByLabel('이메일').fill('signup-e2e@restart-point.com');
  await page.getByRole('button', { name: '인증 코드 받기' }).click();
  for (const [index, digit] of ['1', '2', '3', '4', '5', '6'].entries()) {
    await page.locator('input[inputmode="numeric"]').nth(index).fill(digit);
  }
  await page.getByRole('button', { name: '인증하기' }).click();

  await page.getByLabel('이름').fill('회원가입테스트');
  await page.getByLabel('비밀번호', { exact: true }).fill('password123');
  await page.getByLabel('비밀번호 확인').fill('password456');
  await page.getByRole('button', { name: '회원가입 완료' }).click();

  await expect(page.getByText('비밀번호가 일치하지 않습니다.')).toBeVisible();
});

test('8자 미만 비밀번호는 회원가입이 막힌다', async ({ page }) => {
  await page.route('**/api/v1/auth/email/send', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: null }) });
  });

  await page.route('**/api/v1/auth/email/verify', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: { signupToken: 'mock-signup-token' } }),
    });
  });

  await page.goto('/signup');
  await page.getByLabel('이메일').fill('signup-e2e@restart-point.com');
  await page.getByRole('button', { name: '인증 코드 받기' }).click();
  for (const [index, digit] of ['1', '2', '3', '4', '5', '6'].entries()) {
    await page.locator('input[inputmode="numeric"]').nth(index).fill(digit);
  }
  await page.getByRole('button', { name: '인증하기' }).click();

  await page.getByLabel('이름').fill('회원가입테스트');
  await page.getByLabel('비밀번호', { exact: true }).fill('1234567');
  await page.getByLabel('비밀번호 확인').fill('1234567');
  await page.getByRole('button', { name: '회원가입 완료' }).click();

  await expect(page.getByText('비밀번호는 8자 이상이어야 합니다.')).toBeVisible();
});

test('회원가입 완료 시 홈으로 이동하고 로그인 상태가 반영된다', async ({ page }) => {
  await page.route('**/api/v1/auth/email/send', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: null }) });
  });

  await page.route('**/api/v1/auth/email/verify', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: { signupToken: 'mock-signup-token' } }),
    });
  });

  await page.route('**/api/v1/auth/signup', async (route) => {
    await route.fulfill({
      status: 201,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          accessToken: 'mock-access-token',
          user: signupUser,
        },
        message: '회원가입이 완료되었습니다.',
      }),
    });
  });

  await page.goto('/signup');
  await page.getByLabel('이메일').fill(signupUser.email);
  await page.getByRole('button', { name: '인증 코드 받기' }).click();
  for (const [index, digit] of ['1', '2', '3', '4', '5', '6'].entries()) {
    await page.locator('input[inputmode="numeric"]').nth(index).fill(digit);
  }
  await page.getByRole('button', { name: '인증하기' }).click();

  await page.getByLabel('이름').fill(signupUser.name);
  await page.getByLabel('비밀번호', { exact: true }).fill('password123');
  await page.getByLabel('비밀번호 확인').fill('password123');
  await page.getByRole('button', { name: '회원가입 완료' }).click();

  await expect(page).toHaveURL(/\/$/);
  await expect(page.getByText(signupUser.name)).toBeVisible();
  await expect(page.getByText('미인증')).toBeVisible();
});
