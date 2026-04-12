import { test, expect } from '@playwright/test';

const adminUser = {
  id: 1,
  email: 'admin@restart-point.com',
  name: '관리자',
  role: 'ADMIN',
  certificationStatus: 'APPROVED',
  createdAt: '2026-04-12T00:00:00Z',
};

const normalUser = {
  ...adminUser,
  id: 2,
  email: 'user@test.com',
  name: '일반유저',
  role: 'USER',
};

const seasonsPage = {
  content: [
    {
      id: 1,
      title: '2026 봄 시즌',
      description: '활성 시즌',
      status: 'RECRUITING',
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
    },
  ],
  totalPages: 1,
  totalElements: 1,
  size: 20,
  number: 0,
};

const pendingUsers = [
  {
    id: 10,
    name: '인증대기유저',
    email: 'pending@test.com',
    bootcampName: '멋사',
    bootcampGeneration: '13기',
    createdAt: '2026-04-12T00:00:00Z',
  },
];

const overallDashboard = {
  activeSeasonCount: 1,
  pendingCertifications: 1,
};

const seasonDashboard = {
  teamStats: {
    totalTeams: 4,
    completeTeams: 2,
    incompleteTeams: 1,
    recruitingTeams: 1,
  },
  participantStats: {
    totalParticipants: 12,
    roleDistribution: {
      PLANNER: 3,
      UXUI: 2,
      FRONTEND: 4,
      BACKEND: 3,
    },
  },
  projectStats: {
    totalProjects: 4,
    submittedProjects: 2,
    inProgressProjects: 2,
    submissionRate: 50,
    checkpointMissingCount: 1,
  },
  reviewStats: {
    totalReviews: 8,
    averageScore: 4.2,
    scoreDistribution: {
      excellent: 4,
      good: 3,
      average: 1,
      belowAverage: 0,
    },
  },
  reportStats: {
    totalReports: 4,
    generatedReports: 3,
    generationRate: 75,
  },
  riskTeams: [],
};

function seedPersistedAuth(page: import('@playwright/test').Page, token?: string) {
  return page.addInitScript((accessToken) => {
    if (accessToken) {
      window.localStorage.setItem('accessToken', accessToken);
      window.localStorage.setItem('admin-auth-storage', JSON.stringify({ state: { accessToken }, version: 0 }));
    } else {
      window.localStorage.removeItem('accessToken');
      window.localStorage.removeItem('admin-auth-storage');
    }
  }, token ?? null);
}

async function mockAdminBootstrap(page: import('@playwright/test').Page, options?: {
  me?: unknown;
  loginError?: string;
  loginErrorStatus?: number;
}) {
  const me = options?.me ?? adminUser;

  await page.route('**/api/v1/auth/login', async (route) => {
    if (options?.loginError) {
      await route.fulfill({
        status: options.loginErrorStatus ?? 401,
        contentType: 'application/json',
        body: JSON.stringify({ message: options.loginError }),
      });
      return;
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: { accessToken: 'admin-token', user: me } }),
    });
  });

  await page.route('**/api/v1/users/me', async (route) => {
    await route.fulfill({
      status: me ? 200 : 401,
      contentType: 'application/json',
      body: JSON.stringify(me ? { success: true, data: me } : { message: 'Unauthorized' }),
    });
  });

  await page.route('**/api/v1/admin/seasons**', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: seasonsPage }) });
  });

  await page.route('**/api/v1/admin/users/certifications/pending', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: pendingUsers }) });
  });

  await page.route('**/api/v1/admin/dashboard', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: overallDashboard }) });
  });

  await page.route('**/api/v1/admin/dashboard/seasons/1', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: seasonDashboard }) });
  });
}

test('비로그인 사용자가 관리자 루트 접근 시 로그인 페이지로 이동한다', async ({ page }) => {
  await seedPersistedAuth(page);
  await page.goto('/');
  await expect(page).toHaveURL(/\/login$/);
});

test('관리자 로그인 성공 시 대시보드로 이동한다', async ({ page }) => {
  await seedPersistedAuth(page);
  await mockAdminBootstrap(page);

  await page.goto('/login');
  await page.getByLabel('이메일').fill('admin@restart-point.com');
  await page.getByLabel('비밀번호').fill('test1234');
  await page.getByRole('button', { name: '로그인' }).click();

  await expect(page.getByRole('heading', { name: '대시보드' })).toBeVisible();
  await expect(page.getByRole('heading', { name: '활성 시즌' })).toBeVisible();
  await expect(page.getByRole('heading', { name: '인증 대기' })).toBeVisible();
});

test('잘못된 관리자 로그인 시 에러 문구가 보인다', async ({ page }) => {
  await seedPersistedAuth(page);
  await mockAdminBootstrap(page, { loginError: '이메일 또는 비밀번호가 올바르지 않습니다.', loginErrorStatus: 400 });

  await page.goto('/login');
  await page.getByLabel('이메일').fill('admin@restart-point.com');
  await page.getByLabel('비밀번호').fill('wrong');
  await page.getByRole('button', { name: '로그인' }).click();

  await expect(page.getByText('이메일 또는 비밀번호가 올바르지 않습니다.')).toBeVisible();
});

test('일반 사용자 토큰으로 관리자 페이지 진입 시 로그인 페이지로 되돌아간다', async ({ page }) => {
  await seedPersistedAuth(page, 'user-token');
  await mockAdminBootstrap(page, { me: normalUser });

  await page.goto('/');
  await expect(page).toHaveURL(/\/login$/);
});

test('관리자는 주요 화면인 시즌 관리, 인증 관리, 회원 관리에 접근할 수 있다', async ({ page }) => {
  await seedPersistedAuth(page, 'admin-token');
  await mockAdminBootstrap(page);
  await page.route('**/api/v1/admin/users**', async (route) => {
    const url = new URL(route.request().url());
    if (!url.pathname.endsWith('/admin/users')) {
      await route.fallback();
      return;
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          content: [adminUser],
          totalPages: 1,
          totalElements: 1,
          size: 20,
          number: 0,
        },
      }),
    });
  });

  await page.goto('/seasons');
  await expect(page.getByRole('heading', { name: '시즌 관리' })).toBeVisible();

  await page.goto('/certifications');
  await expect(page.getByRole('heading', { name: '수료 인증 관리' })).toBeVisible();

  await page.goto('/users');
  await expect(page.getByRole('heading', { name: '회원 관리' })).toBeVisible();
});
