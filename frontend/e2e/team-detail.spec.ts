import { test, expect } from '@playwright/test';

const certifiedUser = {
  id: 1,
  email: 'approved@restart-point.com',
  name: '인증유저',
  role: 'USER',
  emailVerified: true,
  certificationStatus: 'APPROVED',
  createdAt: '2026-04-12T00:00:00Z',
  updatedAt: '2026-04-12T00:00:00Z',
};

const leaderUser = {
  ...certifiedUser,
  id: 2,
  email: 'leader@test.com',
  name: '팀리더',
};

const unauthenticatedState = {
  state: {
    user: null,
    accessToken: null,
    isAuthenticated: false,
  },
  version: 0,
};

const certifiedState = {
  state: {
    user: certifiedUser,
    accessToken: 'fake-token',
    isAuthenticated: true,
  },
  version: 0,
};

const leaderState = {
  state: {
    user: leaderUser,
    accessToken: 'fake-token',
    isAuthenticated: true,
  },
  version: 0,
};

const baseTeam = {
  id: 11,
  name: '알파 팀',
  description: '팀 상세 테스트용 설명',
  seasonId: 1,
  seasonTitle: '활성 시즌',
  leaderId: 2,
  leaderName: '팀리더',
  status: 'RECRUITING',
  recruitingPlanner: false,
  recruitingUxui: false,
  recruitingFrontend: true,
  recruitingBackend: true,
  memberCount: 2,
  maxMemberCount: 4,
  members: [
    { id: 1, userId: 2, userName: '팀리더', role: 'BACKEND', status: 'ACCEPTED', createdAt: '2026-04-12T00:00:00Z' },
    { id: 2, userId: 3, userName: '팀원', role: 'FRONTEND', status: 'ACCEPTED', createdAt: '2026-04-12T00:00:00Z' },
  ],
  createdAt: '2026-04-12T00:00:00Z',
};

const teamWithCertifiedMember = {
  ...baseTeam,
  members: [
    ...baseTeam.members,
    { id: 3, userId: 1, userName: '인증유저', role: 'UXUI', status: 'ACCEPTED', createdAt: '2026-04-12T00:00:00Z' },
  ],
  memberCount: 3,
};

const inProgressTeamWithMember = {
  ...teamWithCertifiedMember,
  status: 'IN_PROGRESS',
  recruitingFrontend: false,
  recruitingBackend: false,
};

const applications = [
  {
    id: 101,
    teamId: 11,
    userId: 4,
    userName: '지원자',
    role: 'FRONTEND',
    status: 'PENDING',
    applicationMessage: '프론트엔드로 함께하고 싶습니다.',
    createdAt: '2026-04-12T00:00:00Z',
  },
];

async function mockTeamDetailApis(page: import('@playwright/test').Page, options?: {
  team?: unknown;
  applications?: unknown[];
  unreadCount?: number;
}) {
  const {
    team = baseTeam,
    applications: applicationList = applications,
    unreadCount = 0,
  } = options ?? {};

  await page.route('**/api/v1/teams/11/applications', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: applicationList }) });
  });

  await page.route('**/api/v1/teams/11', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: team }) });
  });

  await page.route('**/api/v1/notifications/unread-count', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: { count: unreadCount } }) });
  });
}

test('팀 상세 정상 조회 시 팀 정보와 모집 역할이 보인다', async ({ page }) => {
  await page.addInitScript((storageValue) => {
    window.localStorage.setItem('auth-storage', JSON.stringify(storageValue));
    window.localStorage.removeItem('accessToken');
  }, unauthenticatedState);

  await mockTeamDetailApis(page);

  await page.goto('/teams/11');

  await expect(page.getByRole('heading', { name: '알파 팀' })).toBeVisible();
  await expect(page.getByRole('main').getByText('팀 상세 테스트용 설명').first()).toBeVisible();
  await expect(page.getByText('모집 중인 역할:')).toBeVisible();
  await expect(page.getByText('프론트엔드')).toBeVisible();
  await expect(page.getByText('백엔드')).toBeVisible();
});

test('존재하지 않는 팀 접근 시 오류 안내가 보인다', async ({ page }) => {
  await page.addInitScript((storageValue) => {
    window.localStorage.setItem('auth-storage', JSON.stringify(storageValue));
    window.localStorage.removeItem('accessToken');
  }, unauthenticatedState);

  await page.route('**/api/v1/teams/999', async (route) => {
    await route.fulfill({ status: 404, contentType: 'application/json', body: JSON.stringify({ success: false, message: '팀을 찾을 수 없습니다.' }) });
  });

  await page.goto('/teams/999');

  await expect(page.getByText('팀 정보를 불러오는데 실패했습니다.')).toBeVisible();
  await expect(page.getByRole('link', { name: '시즌 목록으로' })).toBeVisible();
});

test('비로그인 사용자가 모집중 팀에 접근하면 로그인 필요 안내가 보인다', async ({ page }) => {
  await page.addInitScript((storageValue) => {
    window.localStorage.setItem('auth-storage', JSON.stringify(storageValue));
    window.localStorage.removeItem('accessToken');
  }, unauthenticatedState);

  await mockTeamDetailApis(page);

  await page.goto('/teams/11');

  await expect(page.getByText('팀에 지원하려면')).toBeVisible();
  await expect(page.getByRole('main').getByRole('link', { name: '로그인' })).toBeVisible();
  await expect(page.getByRole('button', { name: '팀에 지원하기' })).not.toBeVisible();
});

test('인증 완료 사용자가 지원 가능 조건이면 팀에 지원하기 버튼이 보인다', async ({ page }) => {
  await page.addInitScript((storageValue) => {
    window.localStorage.setItem('accessToken', 'fake-token');
    window.localStorage.setItem('auth-storage', JSON.stringify(storageValue));
  }, certifiedState);

  await mockTeamDetailApis(page);

  await page.goto('/teams/11');

  await expect(page.getByRole('button', { name: '팀에 지원하기' })).toBeVisible();
});

test('리더 본인이 자기 팀에 접근하면 지원자 탭이 보이고 지원 버튼은 없다', async ({ page }) => {
  await page.addInitScript((storageValue) => {
    window.localStorage.setItem('accessToken', 'fake-token');
    window.localStorage.setItem('auth-storage', JSON.stringify(storageValue));
  }, leaderState);

  await mockTeamDetailApis(page);

  await page.goto('/teams/11');

  await expect(page.getByRole('button', { name: /지원자 \(1\)/ })).toBeVisible();
  await expect(page.getByRole('button', { name: '팀에 지원하기' })).not.toBeVisible();
});

test('이미 팀 멤버인 사용자는 지원 버튼 대신 프로젝트 워크스페이스 버튼이 보인다', async ({ page }) => {
  await page.addInitScript((storageValue) => {
    window.localStorage.setItem('accessToken', 'fake-token');
    window.localStorage.setItem('auth-storage', JSON.stringify(storageValue));
  }, certifiedState);

  await mockTeamDetailApis(page, { team: inProgressTeamWithMember });

  await page.goto('/teams/11');

  await expect(page.getByRole('button', { name: '팀에 지원하기' })).not.toBeVisible();
  await expect(page.getByRole('link', { name: '프로젝트 워크스페이스' })).toHaveAttribute('href', '/teams/11/project');
});

test('지원 모달에서 역할 없이 제출하면 에러가 보인다', async ({ page }) => {
  await page.addInitScript((storageValue) => {
    window.localStorage.setItem('accessToken', 'fake-token');
    window.localStorage.setItem('auth-storage', JSON.stringify(storageValue));
  }, certifiedState);

  await mockTeamDetailApis(page);

  await page.goto('/teams/11');
  await page.getByRole('button', { name: '팀에 지원하기' }).click();
  await page.getByRole('button', { name: '지원하기', exact: true }).click();

  await expect(page.getByText('지원할 역할을 선택해주세요.')).toBeVisible();
});
