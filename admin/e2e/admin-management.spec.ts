import { test, expect } from '@playwright/test';

const adminUser = {
  id: 1,
  email: 'admin@restart-point.com',
  name: '관리자',
  role: 'ADMIN',
  certificationStatus: 'APPROVED',
  createdAt: '2026-04-12T00:00:00Z',
};

const allUsersPage = {
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
  totalPages: 2,
  totalElements: 3,
  size: 20,
  number: 0,
  first: true,
  last: false,
};

const nextPageUsers = {
  content: [
    {
      id: 3,
      email: 'member@test.com',
      name: '다음페이지유저',
      role: 'USER',
      certificationStatus: 'REJECTED',
      bootcampName: '항해',
      bootcampGeneration: '5기',
      createdAt: '2026-04-10T00:00:00Z',
    },
  ],
  totalPages: 2,
  totalElements: 3,
  size: 20,
  number: 1,
  first: false,
  last: true,
};

const adminOnlyUsers = {
  ...allUsersPage,
  content: [allUsersPage.content[0]],
  totalElements: 1,
  totalPages: 1,
  last: true,
};

const approvedOnlyUsers = {
  ...allUsersPage,
  content: [allUsersPage.content[0]],
  totalElements: 1,
  totalPages: 1,
  last: true,
};

const searchedUsers = {
  ...allUsersPage,
  content: [allUsersPage.content[1]],
  totalElements: 1,
  totalPages: 1,
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
    window.localStorage.setItem(
      'admin-auth-storage',
      JSON.stringify({ state: { accessToken: 'admin-token' }, version: 0 })
    );
  });
}

async function mockAdminManagement(
  page: import('@playwright/test').Page,
  options?: {
    pendingUsers?: typeof pendingUsers;
    forceUsersError?: boolean;
  }
) {
  let certificationList = options?.pendingUsers ?? pendingUsers;

  await page.route('**/api/v1/users/me', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: adminUser }),
    });
  });

  await page.route('**/api/v1/admin/users/certifications/pending', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: certificationList }),
    });
  });

  await page.route('**/api/v1/admin/users/*/certification/approve', async (route) => {
    certificationList = [];
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: { id: 2 } }),
    });
  });

  await page.route('**/api/v1/admin/users/*/certification/reject', async (route) => {
    certificationList = [];
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: { id: 2 } }),
    });
  });

  await page.route('**/api/v1/admin/users/*/role', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: { id: 2, role: 'ADMIN' } }),
    });
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

    if (options?.forceUsersError) {
      await route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({ success: false, message: '회원 목록 조회 실패' }),
      });
      return;
    }

    const keyword = url.searchParams.get('keyword');
    const role = url.searchParams.get('role');
    const certificationStatus = url.searchParams.get('certificationStatus');
    const pageParam = url.searchParams.get('page');

    let responseData = allUsersPage;

    if (keyword === '일반유저' || keyword === 'user@test.com') {
      responseData = searchedUsers;
    } else if (role === 'ADMIN') {
      responseData = adminOnlyUsers;
    } else if (certificationStatus === 'APPROVED') {
      responseData = approvedOnlyUsers;
    } else if (pageParam === '1') {
      responseData = nextPageUsers;
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: responseData }),
    });
  });
}

test('회원 관리 화면에서 본인 행은 (본인) 표시가 보인다', async ({ page }) => {
  await seedPersistedAuth(page);
  await mockAdminManagement(page);

  await page.goto('/users');

  await expect(page.getByRole('heading', { name: '회원 관리' })).toBeVisible();
  await expect(page.getByText('(본인)')).toBeVisible();
  await expect(page.getByText('총 3명')).toBeVisible();
});

test('회원 역할 변경 모달을 열고 관리자 변경 버튼을 볼 수 있다', async ({ page }) => {
  await seedPersistedAuth(page);
  await mockAdminManagement(page);

  await page.goto('/users');
  await page.getByRole('button', { name: '역할 변경' }).click();

  await expect(page.getByRole('heading', { name: '역할 변경' })).toBeVisible();
  await expect(page.getByRole('button', { name: '관리자로 변경' })).toBeVisible();
});

test('회원 이름 또는 이메일 검색이 동작한다', async ({ page }) => {
  await seedPersistedAuth(page);
  await mockAdminManagement(page);

  await page.goto('/users');
  await page.getByPlaceholder('이름 또는 이메일로 검색...').fill('일반유저');
  await page.getByRole('button', { name: '검색' }).click();

  await expect(page.getByRole('cell', { name: '일반유저' })).toBeVisible();
  await expect(page.getByRole('cell', { name: 'user@test.com' })).toBeVisible();
  await expect(page.getByRole('cell', { name: 'admin@restart-point.com' })).not.toBeVisible();
});

test('역할 필터가 동작한다', async ({ page }) => {
  await seedPersistedAuth(page);
  await mockAdminManagement(page);

  await page.goto('/users');
  await page.selectOption('select', 'ADMIN');

  await expect(page.getByRole('row', { name: /admin@restart-point\.com/ })).toBeVisible();
  await expect(page.getByRole('row', { name: /user@test\.com/ })).not.toBeVisible();
});

test('인증 상태 필터가 동작한다', async ({ page }) => {
  await seedPersistedAuth(page);
  await mockAdminManagement(page);

  await page.goto('/users');
  await page.locator('select').nth(1).selectOption('APPROVED');

  await expect(page.getByRole('row', { name: /admin@restart-point\.com/ })).toBeVisible();
  await expect(page.getByRole('row', { name: /user@test\.com/ })).not.toBeVisible();
});

test('페이지네이션 다음 버튼이 동작한다', async ({ page }) => {
  await seedPersistedAuth(page);
  await mockAdminManagement(page);

  await page.goto('/users');
  await page.getByRole('button', { name: '다음' }).click();

  await expect(page.getByText('다음페이지유저')).toBeVisible();
  await expect(page.getByText('2 / 2')).toBeVisible();
});

test('다른 사용자 삭제 모달에서 삭제할 수 있다', async ({ page }) => {
  await seedPersistedAuth(page);
  await mockAdminManagement(page);

  await page.goto('/users');
  await page.getByRole('button', { name: '삭제' }).click();

  await expect(page.getByRole('heading', { name: '회원 삭제' })).toBeVisible();
  await page.locator('[role="dialog"], .fixed.inset-0').getByRole('button', { name: '삭제' }).click();

  await expect(page.getByRole('heading', { name: '회원 관리' })).toBeVisible();
});

test('회원 목록 조회 실패 시 에러 문구가 보인다', async ({ page }) => {
  await seedPersistedAuth(page);
  await mockAdminManagement(page, { forceUsersError: true });

  await page.goto('/users');

  await expect(page.getByText('회원 목록을 불러오는데 실패했습니다.')).toBeVisible();
});

test('인증 관리 화면에서 승인 버튼을 누르면 empty state로 갱신된다', async ({ page }) => {
  await seedPersistedAuth(page);
  await mockAdminManagement(page);

  await page.addInitScript(() => {
    window.confirm = () => true;
  });

  await page.goto('/certifications');
  await page.getByRole('button', { name: '승인' }).click();

  await expect(page.getByText('대기 중인 인증 요청이 없습니다.')).toBeVisible();
  await expect(page.getByText('0건')).toBeVisible();
});

test('인증 관리 화면에서 거절 버튼을 누르면 empty state로 갱신된다', async ({ page }) => {
  await seedPersistedAuth(page);
  await mockAdminManagement(page);

  await page.addInitScript(() => {
    window.confirm = () => true;
  });

  await page.goto('/certifications');
  await page.getByRole('button', { name: '거절' }).click();

  await expect(page.getByText('대기 중인 인증 요청이 없습니다.')).toBeVisible();
  await expect(page.getByText('0건')).toBeVisible();
});

test('인증 승인 confirm을 취소하면 요청이 실행되지 않는다', async ({ page }) => {
  await seedPersistedAuth(page);
  await mockAdminManagement(page);

  await page.addInitScript(() => {
    window.confirm = () => false;
  });

  await page.goto('/certifications');
  await page.getByRole('button', { name: '승인' }).click();

  await expect(page.getByText('일반유저')).toBeVisible();
  await expect(page.getByText('1건')).toBeVisible();
});

test('인증 대기 건수가 0건이면 empty state가 보인다', async ({ page }) => {
  await seedPersistedAuth(page);
  await mockAdminManagement(page, { pendingUsers: [] });

  await page.goto('/certifications');

  await expect(page.getByText('대기 중인 인증 요청이 없습니다.')).toBeVisible();
  await expect(page.getByText('0건')).toBeVisible();
});
