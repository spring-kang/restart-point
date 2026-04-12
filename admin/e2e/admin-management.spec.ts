import { test, expect } from '@playwright/test';

const adminUser = {
  id: 1,
  email: 'admin@restart-point.com',
  name: '관리자',
  role: 'ADMIN',
  certificationStatus: 'APPROVED',
  createdAt: '2026-04-12T00:00:00Z',
};

const userList = {
  content: [
    {
      id: 1,
      email: 'admin@restart-point.com',
      name: '관리자',
      role: 'ADMIN',
      certificationStatus: 'APPROVED',
      bootcampName: '멋사',
      bootcampGeneration: '13기',
      createdAt: '2026-04-12T00:00:00Z',
    },
    {
      id: 2,
      email: 'user@test.com',
      name: '일반유저',
      role: 'USER',
      certificationStatus: 'PENDING',
      bootcampName: '멋사',
      bootcampGeneration: '13기',
      createdAt: '2026-04-12T00:00:00Z',
    },
  ],
  totalPages: 1,
  totalElements: 2,
  size: 20,
  number: 0,
  first: true,
  last: true,
};

const pendingUsers = [
  {
    id: 2,
    name: '일반유저',
    email: 'user@test.com',
    certificationStatus: 'PENDING',
    bootcampName: '멋사',
    bootcampGeneration: '13기',
    graduationDate: '2026-03-01',
    createdAt: '2026-04-12T00:00:00Z',
  },
];

function seedPersistedAuth(page: import('@playwright/test').Page) {
  return page.addInitScript(() => {
    window.localStorage.setItem('accessToken', 'admin-token');
    window.localStorage.setItem('admin-auth-storage', JSON.stringify({ state: { accessToken: 'admin-token' }, version: 0 }));
  });
}

async function mockAdminManagement(page: import('@playwright/test').Page) {
  await page.route('**/api/v1/users/me', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: adminUser }) });
  });

  await page.route('**/api/v1/admin/users/certifications/pending', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: pendingUsers }) });
  });

  await page.route('**/api/v1/admin/users/*/certification/approve', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: { id: 2 } }) });
  });

  await page.route('**/api/v1/admin/users/*/certification/reject', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: { id: 2 } }) });
  });

  await page.route('**/api/v1/admin/users/*/role', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: { id: 2, role: 'ADMIN' } }) });
  });

  await page.route('**/api/v1/admin/users/*', async (route) => {
    if (route.request().method() === 'DELETE') {
      await route.fulfill({ status: 204, body: '' });
      return;
    }
    await route.fallback();
  });

  await page.route('**/api/v1/admin/users**', async (route) => {
    const url = new URL(route.request().url());
    if (!url.pathname.endsWith('/admin/users')) {
      await route.fallback();
      return;
    }

    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: userList }) });
  });
}

test('회원 관리 화면에서 본인 행은 (본인) 표시가 보인다', async ({ page }) => {
  await seedPersistedAuth(page);
  await mockAdminManagement(page);

  await page.goto('/users');

  await expect(page.getByRole('heading', { name: '회원 관리' })).toBeVisible();
  await expect(page.getByText('(본인)')).toBeVisible();
  await expect(page.getByText('총 2명')).toBeVisible();
});

test('회원 역할 변경 모달을 열고 관리자 변경 버튼을 볼 수 있다', async ({ page }) => {
  await seedPersistedAuth(page);
  await mockAdminManagement(page);

  await page.goto('/users');
  await page.getByRole('button', { name: '역할 변경' }).click();

  await expect(page.getByRole('heading', { name: '역할 변경' })).toBeVisible();
  await expect(page.getByRole('button', { name: '관리자로 변경' })).toBeVisible();
});

test('인증 관리 화면에서 승인 버튼을 누르면 목록이 유지된 채 재조회된다', async ({ page }) => {
  await seedPersistedAuth(page);
  await mockAdminManagement(page);

  await page.addInitScript(() => {
    window.confirm = () => true;
  });

  await page.goto('/certifications');
  await page.getByRole('button', { name: '승인' }).click();

  await expect(page.getByRole('heading', { name: '수료 인증 관리' })).toBeVisible();
  await expect(page.getByText('대기 중인 인증 요청')).toBeVisible();
});

test('인증 관리 화면에서 거절 버튼을 누르면 목록이 유지된 채 재조회된다', async ({ page }) => {
  await seedPersistedAuth(page);
  await mockAdminManagement(page);

  await page.addInitScript(() => {
    window.confirm = () => true;
  });

  await page.goto('/certifications');
  await page.getByRole('button', { name: '거절' }).click();

  await expect(page.getByRole('heading', { name: '수료 인증 관리' })).toBeVisible();
  await expect(page.getByText('일반유저')).toBeVisible();
});
