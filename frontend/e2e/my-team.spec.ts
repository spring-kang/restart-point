import { test, expect } from '@playwright/test';

const uncertifiedUser = {
  id: 1,
  email: 'user@restart-point.com',
  name: '미인증유저',
  role: 'USER',
  emailVerified: true,
  certificationStatus: 'NONE',
  createdAt: '2026-04-12T00:00:00Z',
  updatedAt: '2026-04-12T00:00:00Z',
};

const certifiedUser = {
  ...uncertifiedUser,
  email: 'approved@restart-point.com',
  name: '인증유저',
  certificationStatus: 'APPROVED',
};

const leaderTeam = {
  id: 11,
  name: '리더 팀',
  description: '내가 만든 팀 설명',
  seasonId: 1,
  seasonTitle: '테스트 시즌',
  leaderId: 1,
  leaderName: '인증유저',
  status: 'RECRUITING',
  recruitingPlanner: false,
  recruitingUxui: false,
  recruitingFrontend: true,
  recruitingBackend: true,
  memberCount: 2,
  maxMemberCount: 4,
  createdAt: '2026-04-12T00:00:00Z',
};

const memberTeam = {
  id: 12,
  name: '소속 팀',
  description: '멤버로 참여 중인 팀',
  seasonId: 1,
  seasonTitle: '테스트 시즌',
  leaderId: 2,
  leaderName: '다른리더',
  status: 'IN_PROGRESS',
  recruitingPlanner: false,
  recruitingUxui: false,
  recruitingFrontend: false,
  recruitingBackend: false,
  memberCount: 4,
  maxMemberCount: 4,
  createdAt: '2026-04-12T00:00:00Z',
};

const application = {
  id: 100,
  teamId: 13,
  userId: 1,
  userName: '인증유저',
  role: 'BACKEND',
  status: 'PENDING',
  applicationMessage: '백엔드로 기여하고 싶습니다.',
  createdAt: '2026-04-12T00:00:00Z',
};

const createAuthStorage = (user: typeof uncertifiedUser | typeof certifiedUser | null) => ({
  state: {
    user,
    accessToken: user ? 'fake-token' : null,
    isAuthenticated: Boolean(user),
  },
  version: 0,
});

async function mockMyTeamApis(page: import('@playwright/test').Page, options?: {
  myTeams?: unknown[];
  memberTeams?: unknown[];
  applications?: unknown[];
  unreadCount?: number;
}) {
  const {
    myTeams = [],
    memberTeams = [],
    applications = [],
    unreadCount = 0,
  } = options ?? {};

  await page.route('**/api/v1/users/me/teams', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: myTeams }) });
  });

  await page.route('**/api/v1/users/me/teams/member', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: memberTeams }) });
  });

  await page.route('**/api/v1/users/me/applications', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: applications }) });
  });

  await page.route('**/api/v1/notifications/unread-count', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: { count: unreadCount } }) });
  });
}

test('비로그인 사용자는 로그인 페이지로 이동한다', async ({ page }) => {
  await page.addInitScript((storageValue) => {
    window.localStorage.setItem('auth-storage', JSON.stringify(storageValue));
    window.localStorage.removeItem('accessToken');
  }, createAuthStorage(null));

  await page.goto('/my-team');

  await expect(page).toHaveURL(/\/login$/);
  await expect(page.getByRole('heading', { name: '로그인' })).toBeVisible();
});

test('인증 완료 사용자가 데이터가 없으면 팀 둘러보기 CTA가 보인다', async ({ page }) => {
  await page.addInitScript((storageValue) => {
    window.localStorage.setItem('accessToken', 'fake-token');
    window.localStorage.setItem('auth-storage', JSON.stringify(storageValue));
  }, createAuthStorage(certifiedUser));

  await mockMyTeamApis(page);

  await page.goto('/my-team');

  await expect(page.getByRole('heading', { name: '내 팀' })).toBeVisible();
  await expect(page.getByText('아직 팀이 없습니다')).toBeVisible();
  await expect(page.getByRole('link', { name: '팀 둘러보기' })).toBeVisible();
  await expect(page.getByRole('link', { name: '수료 인증하기' })).not.toBeVisible();
});

test('미인증 사용자가 데이터가 없으면 수료 인증하기 CTA가 보인다', async ({ page }) => {
  await page.addInitScript((storageValue) => {
    window.localStorage.setItem('accessToken', 'fake-token');
    window.localStorage.setItem('auth-storage', JSON.stringify(storageValue));
  }, createAuthStorage(uncertifiedUser));

  await mockMyTeamApis(page);

  await page.goto('/my-team');

  await expect(page.getByText('수료 인증을 완료하면 팀에 참여할 수 있습니다.')).toBeVisible();
  await expect(page.getByRole('link', { name: '수료 인증하기' })).toBeVisible();
});

test('내가 만든 팀, 소속 팀, 지원 현황 섹션이 각각 노출된다', async ({ page }) => {
  await page.addInitScript((storageValue) => {
    window.localStorage.setItem('accessToken', 'fake-token');
    window.localStorage.setItem('auth-storage', JSON.stringify(storageValue));
  }, createAuthStorage(certifiedUser));

  await mockMyTeamApis(page, {
    myTeams: [leaderTeam],
    memberTeams: [memberTeam],
    applications: [application],
  });

  await page.goto('/my-team');

  await expect(page.getByRole('heading', { name: '내가 만든 팀' })).toBeVisible();
  await expect(page.getByRole('heading', { name: '소속 팀' }).first()).toBeVisible();
  await expect(page.getByRole('heading', { name: '지원 현황' })).toBeVisible();
  await expect(page.getByRole('heading', { name: '리더 팀' })).toBeVisible();
  await expect(page.getByRole('heading', { name: '소속 팀' }).nth(1)).toBeVisible();
  await expect(page.getByText('백엔드로 기여하고 싶습니다.')).toBeVisible();
  await expect(page.getByRole('link', { name: '팀 보기' })).toHaveAttribute('href', '/teams/13');
});

test('프로젝트 진행 중인 소속 팀은 프로젝트 버튼이 보인다', async ({ page }) => {
  await page.addInitScript((storageValue) => {
    window.localStorage.setItem('accessToken', 'fake-token');
    window.localStorage.setItem('auth-storage', JSON.stringify(storageValue));
  }, createAuthStorage(certifiedUser));

  await mockMyTeamApis(page, {
    memberTeams: [memberTeam],
  });

  await page.goto('/my-team');

  await expect(page.getByRole('link', { name: '프로젝트', exact: true })).toHaveAttribute('href', '/teams/12/project');
});
