import { test, expect } from '@playwright/test';

const approvedUser = {
  id: 10,
  email: 'java7ang@gmail.com',
  name: '프로필유저',
  role: 'USER',
  emailVerified: true,
  certificationStatus: 'APPROVED',
  createdAt: '2026-04-12T00:00:00Z',
  updatedAt: '2026-04-12T00:00:00Z',
};

const unauthenticatedState = {
  state: {
    user: null,
    accessToken: null,
    isAuthenticated: false,
  },
  version: 0,
};

const authenticatedState = {
  state: {
    user: approvedUser,
    accessToken: 'fake-token',
    isAuthenticated: true,
  },
  version: 0,
};

const existingProfile = {
  id: 1,
  userId: 10,
  jobRole: 'BACKEND',
  techStacks: ['Java', 'Spring Boot'],
  portfolioUrl: 'https://portfolio.example.com',
  interestedDomains: ['교육', 'AI/ML'],
  availableHoursPerWeek: 12,
  collaborationStyle: 'COLLABORATIVE',
  improvementGoal: '코드 리뷰 역량 향상',
  preferredDifficulty: 'INTERMEDIATE',
  introduction: '백엔드 개발자입니다.',
  createdAt: '2026-04-12T00:00:00Z',
  updatedAt: '2026-04-12T00:00:00Z',
};

function setAuth(page: import('@playwright/test').Page, storageValue: unknown, authenticated = false) {
  return page.addInitScript(({ value, authenticated }) => {
    window.localStorage.setItem('auth-storage', JSON.stringify(value));
    if (authenticated) {
      window.localStorage.setItem('accessToken', 'fake-token');
    } else {
      window.localStorage.removeItem('accessToken');
    }
  }, { value: storageValue, authenticated });
}

async function mockProfileApis(page: import('@playwright/test').Page, profile: unknown = existingProfile) {
  await page.route('**/api/v1/users/me/profile', async (route) => {
    if (route.request().method() === 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: profile }),
      });
      return;
    }

    if (route.request().method() === 'PUT') {
      const body = route.request().postDataJSON() as Record<string, unknown>;
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            id: 1,
            userId: 10,
            ...body,
            createdAt: '2026-04-12T00:00:00Z',
            updatedAt: '2026-04-12T01:00:00Z',
          },
        }),
      });
    }
  });
}

test('비로그인 사용자는 프로필 페이지에서 로그인 페이지로 이동한다', async ({ page }) => {
  await setAuth(page, unauthenticatedState);
  await page.goto('/profile');
  await expect(page).toHaveURL(/\/login$/);
});

test('기존 프로필이 있으면 주요 정보가 폼에 채워진다', async ({ page }) => {
  await setAuth(page, authenticatedState, true);
  await mockProfileApis(page);

  await page.goto('/profile');

  await expect(page.getByRole('heading', { name: '프로필 수정' })).toBeVisible();
  await expect(page.getByRole('radio', { name: '백엔드 개발자' })).toBeChecked();
  await expect(page.getByRole('button', { name: 'Java' })).toHaveClass(/bg-primary-500/);
  await expect(page.getByRole('button', { name: '교육' })).toHaveClass(/bg-secondary-500/);
  await expect(page.getByPlaceholder('https://...')).toHaveValue('https://portfolio.example.com');
  await expect(page.getByPlaceholder('예: 20')).toHaveValue('12');
  await expect(page.getByPlaceholder('예: 협업 능력, 코드 리뷰 경험')).toHaveValue('코드 리뷰 역량 향상');
  await expect(page.getByPlaceholder('팀원들에게 보여줄 간단한 자기소개를 작성해주세요.')).toHaveValue('백엔드 개발자입니다.');
});

test('역할 없이 저장하면 검증 에러가 보인다', async ({ page }) => {
  await setAuth(page, authenticatedState, true);
  await mockProfileApis(page, null);

  await page.goto('/profile');
  await page.getByRole('button', { name: '프로필 저장' }).click();

  await expect(page.getByText('역할을 선택해주세요.')).toBeVisible();
});

test('프로필 저장 성공 시 성공 메시지가 보인다', async ({ page }) => {
  await setAuth(page, authenticatedState, true);
  await mockProfileApis(page, null);

  await page.goto('/profile');
  await page.getByText('프론트엔드 개발자', { exact: true }).click();
  await page.getByRole('button', { name: 'React' }).click();
  await page.getByRole('button', { name: 'B2B SaaS' }).click();
  await page.getByPlaceholder('예: 20').fill('15');
  await page.getByRole('button', { name: '프로필 저장' }).click();

  await expect(page.getByText('프로필이 저장되었습니다.')).toBeVisible();
  await expect(page.getByRole('heading', { name: '프로필 수정' })).toBeVisible();
});
