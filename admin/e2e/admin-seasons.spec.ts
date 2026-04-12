import { test, expect } from '@playwright/test';

const adminUser = {
  id: 1,
  email: 'admin@restart-point.com',
  name: '관리자',
  role: 'ADMIN',
  certificationStatus: 'APPROVED',
  createdAt: '2026-04-12T00:00:00Z',
};

const seasonsPage = {
  content: [
    {
      id: 1,
      title: '2026 봄 시즌',
      description: '초안 시즌',
      status: 'DRAFT',
      recruitmentStartAt: '2026-04-01T00:00:00',
      recruitmentEndAt: '2026-04-10T00:00:00',
      teamBuildingStartAt: '2026-04-11T00:00:00',
      teamBuildingEndAt: '2026-04-15T00:00:00',
      projectStartAt: '2026-04-16T00:00:00',
      projectEndAt: '2026-04-25T00:00:00',
      reviewStartAt: '2026-04-26T00:00:00',
      reviewEndAt: '2026-04-30T00:00:00',
      expertReviewWeight: 70,
      candidateReviewWeight: 30,
      createdAt: '2026-04-01T00:00:00Z',
      updatedAt: '2026-04-01T00:00:00Z',
    },
    {
      id: 2,
      title: '2026 여름 시즌',
      description: '진행 중 시즌',
      status: 'RECRUITING',
      recruitmentStartAt: '2026-05-01T00:00:00',
      recruitmentEndAt: '2026-05-10T00:00:00',
      teamBuildingStartAt: '2026-05-11T00:00:00',
      teamBuildingEndAt: '2026-05-15T00:00:00',
      projectStartAt: '2026-05-16T00:00:00',
      projectEndAt: '2026-05-25T00:00:00',
      reviewStartAt: '2026-05-26T00:00:00',
      reviewEndAt: '2026-05-30T00:00:00',
      expertReviewWeight: 60,
      candidateReviewWeight: 40,
      currentPhase: '참여자 모집 중',
      createdAt: '2026-05-01T00:00:00Z',
      updatedAt: '2026-05-01T00:00:00Z',
    },
  ],
  totalPages: 1,
  totalElements: 2,
  size: 20,
  number: 0,
};

function seedPersistedAuth(page: import('@playwright/test').Page) {
  return page.addInitScript(() => {
    window.localStorage.setItem('accessToken', 'admin-token');
    window.localStorage.setItem('admin-auth-storage', JSON.stringify({ state: { accessToken: 'admin-token' }, version: 0 }));
  });
}

async function mockSeasonApis(page: import('@playwright/test').Page) {
  await page.route('**/api/v1/users/me', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: adminUser }) });
  });

  await page.route('**/api/v1/admin/seasons/*/status', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: { id: 2, status: 'TEAM_BUILDING' } }) });
  });

  await page.route('**/api/v1/admin/seasons/*', async (route) => {
    if (route.request().method() === 'PUT') {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: { id: 1 } }) });
      return;
    }
    if (route.request().method() === 'DELETE') {
      await route.fulfill({ status: 204, body: '' });
      return;
    }
    await route.fallback();
  });

  await page.route('**/api/v1/admin/seasons**', async (route) => {
    const method = route.request().method();
    const url = new URL(route.request().url());

    if (method === 'POST' && url.pathname.endsWith('/admin/seasons')) {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: { id: 3 } }) });
      return;
    }

    if (method === 'GET' && url.pathname.endsWith('/admin/seasons')) {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: seasonsPage }) });
      return;
    }

    await route.fallback();
  });
}

test('시즌 목록과 상태 필터가 보인다', async ({ page }) => {
  await seedPersistedAuth(page);
  await mockSeasonApis(page);

  await page.goto('/seasons');

  await expect(page.getByRole('heading', { name: '시즌 관리' })).toBeVisible();
  await expect(page.getByText('2026 봄 시즌')).toBeVisible();
  await expect(page.getByText('2026 여름 시즌')).toBeVisible();

  await page.getByRole('button', { name: '초안 (1)' }).click();
  await expect(page.getByText('2026 봄 시즌')).toBeVisible();
});

test('새 시즌 모달에서 필수값을 입력해 생성할 수 있다', async ({ page }) => {
  await seedPersistedAuth(page);
  await mockSeasonApis(page);

  await page.goto('/seasons');
  await page.getByRole('button', { name: '새 시즌' }).click();

  await expect(page.getByRole('heading', { name: '새 시즌 만들기' })).toBeVisible();
  await page.getByPlaceholder('예: 2024 봄 시즌').fill('2026 가을 시즌');
  await page.locator('input[type="date"]').nth(0).fill('2026-09-01');
  await page.locator('input[type="date"]').nth(1).fill('2026-09-10');
  await page.locator('input[type="date"]').nth(2).fill('2026-09-11');
  await page.locator('input[type="date"]').nth(3).fill('2026-09-15');
  await page.locator('input[type="date"]').nth(4).fill('2026-09-16');
  await page.locator('input[type="date"]').nth(5).fill('2026-09-25');
  await page.locator('input[type="date"]').nth(6).fill('2026-09-26');
  await page.locator('input[type="date"]').nth(7).fill('2026-09-30');
  await page.getByRole('button', { name: '생성' }).click();

  await expect(page.getByRole('heading', { name: '시즌 관리' })).toBeVisible();
});

test('기존 시즌 수정 모달에서 제목을 바꿀 수 있다', async ({ page }) => {
  await seedPersistedAuth(page);
  await mockSeasonApis(page);

  await page.goto('/seasons');
  await page.getByTitle('수정').first().click();

  await expect(page.getByRole('heading', { name: '시즌 수정' })).toBeVisible();
  await page.getByPlaceholder('예: 2024 봄 시즌').fill('2026 봄 시즌 수정');
  await page.locator('form').getByRole('button', { name: '수정' }).click();

  await expect(page.getByRole('heading', { name: '시즌 관리' })).toBeVisible();
});

test('다음 상태 버튼으로 시즌 상태 전이 액션을 실행할 수 있다', async ({ page }) => {
  await seedPersistedAuth(page);
  await mockSeasonApis(page);

  await page.goto('/seasons');
  await page.getByRole('button', { name: '팀빌딩' }).first().click();

  await expect(page.getByRole('heading', { name: '시즌 관리' })).toBeVisible();
});

test('초안 시즌은 삭제 버튼이 보이고 삭제 확인 후 목록 화면에 남는다', async ({ page }) => {
  await seedPersistedAuth(page);
  await mockSeasonApis(page);

  await page.addInitScript(() => {
    window.confirm = () => true;
  });

  await page.goto('/seasons');
  await page.getByTitle('삭제').click();

  await expect(page.getByRole('heading', { name: '시즌 관리' })).toBeVisible();
  await expect(page.getByText('2026 봄 시즌')).toBeVisible();
});
