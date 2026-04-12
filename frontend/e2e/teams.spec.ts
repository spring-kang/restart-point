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
    user: certifiedUser,
    accessToken: 'fake-token',
    isAuthenticated: true,
  },
  version: 0,
};

const activeSeason = {
  id: 1,
  title: '활성 시즌',
  description: '팀 찾기 테스트용 시즌',
  status: 'TEAM_BUILDING',
  recruitmentStartAt: '2026-04-01T00:00:00',
  recruitmentEndAt: '2026-04-05T00:00:00',
  teamBuildingStartAt: '2026-04-06T00:00:00',
  teamBuildingEndAt: '2026-04-10T00:00:00',
  projectStartAt: '2026-04-11T00:00:00',
  projectEndAt: '2026-04-20T00:00:00',
  reviewStartAt: '2026-04-21T00:00:00',
  reviewEndAt: '2026-04-25T00:00:00',
  expertReviewWeight: 70,
  candidateReviewWeight: 30,
  currentPhase: '팀빌딩 진행 중',
  canJoin: true,
};

const teams = [
  {
    id: 11,
    name: '알파 팀',
    description: '프론트 중심 팀',
    seasonId: 1,
    seasonTitle: '활성 시즌',
    leaderId: 2,
    leaderName: '알파리더',
    status: 'RECRUITING',
    recruitingPlanner: false,
    recruitingUxui: false,
    recruitingFrontend: true,
    recruitingBackend: false,
    memberCount: 2,
    maxMemberCount: 4,
    createdAt: '2026-04-12T00:00:00Z',
  },
  {
    id: 12,
    name: '베타 팀',
    description: '백엔드 중심 팀',
    seasonId: 1,
    seasonTitle: '활성 시즌',
    leaderId: 3,
    leaderName: '베타리더',
    status: 'RECRUITING',
    recruitingPlanner: false,
    recruitingUxui: false,
    recruitingFrontend: false,
    recruitingBackend: true,
    memberCount: 3,
    maxMemberCount: 4,
    createdAt: '2026-04-12T00:00:00Z',
  },
];

async function mockTeamPageApis(page: import('@playwright/test').Page, options?: {
  activeSeasons?: unknown[];
  season?: unknown;
  teamsBySeason?: unknown[];
  recruitingTeams?: unknown[];
  unreadCount?: number;
  createdTeam?: Record<string, unknown>;
  teamRecommendations?: unknown[];
  teamRecommendationError?: { message?: string; errorCode?: string; status?: number };
}) {
  const {
    activeSeasons = [activeSeason],
    season = activeSeason,
    teamsBySeason = teams,
    recruitingTeams = teams,
    unreadCount = 0,
    createdTeam = {
      id: 99,
      name: '새 팀',
      description: '새로 만든 팀 설명',
      seasonId: 1,
      seasonTitle: '활성 시즌',
      leaderId: 1,
      leaderName: '인증유저',
      status: 'RECRUITING',
      recruitingPlanner: false,
      recruitingUxui: false,
      recruitingFrontend: true,
      recruitingBackend: true,
      memberCount: 1,
      maxMemberCount: 4,
      members: [
        { id: 1, userId: 1, userName: '인증유저', role: 'PLANNER', status: 'ACCEPTED', createdAt: '2026-04-12T00:00:00Z' },
      ],
      createdAt: '2026-04-12T00:00:00Z',
    },
    teamRecommendations = [
      {
        team: teams[0],
        matchScore: 92,
        reasons: ['프론트엔드 역할과 잘 맞습니다.', '협업 스타일이 유사합니다.'],
        balanceAnalysis: '프론트엔드 역량을 보강해 팀 밸런스를 높일 수 있습니다.',
        scheduleRisk: 'LOW',
        missingRoles: ['프론트엔드'],
      },
      {
        team: teams[1],
        matchScore: 78,
        reasons: ['백엔드 경험이 도움됩니다.'],
        balanceAnalysis: '백엔드 구조 설계에 기여할 수 있습니다.',
        scheduleRisk: 'MEDIUM',
        missingRoles: ['백엔드'],
      },
    ],
    teamRecommendationError,
  } = options ?? {};

  await page.route('**/api/v1/seasons/active', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: activeSeasons }) });
  });

  await page.route('**/api/v1/seasons/1', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: season }) });
  });

  await page.route('**/api/v1/seasons/1/teams/recruiting', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: recruitingTeams }) });
  });

  await page.route('**/api/v1/seasons/1/teams', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: teamsBySeason }) });
  });

  await page.route('**/api/v1/matching/teams**', async (route) => {
    if (teamRecommendationError) {
      await route.fulfill({
        status: teamRecommendationError.status ?? 400,
        contentType: 'application/json',
        body: JSON.stringify({
          success: false,
          message: teamRecommendationError.message,
          errorCode: teamRecommendationError.errorCode,
        }),
      });
      return;
    }

    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: teamRecommendations }) });
  });

  await page.route('**/api/v1/teams', async (route) => {
    if (route.request().method() !== 'POST') {
      await route.fallback();
      return;
    }

    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: createdTeam }) });
  });

  await page.route('**/api/v1/teams/99/applications', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: [] }) });
  });

  await page.route('**/api/v1/teams/99', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: createdTeam }) });
  });

  await page.route('**/api/v1/notifications/unread-count', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: { count: unreadCount } }) });
  });
}

test('활성 시즌이 없으면 empty state가 보인다', async ({ page }) => {
  await page.addInitScript((storageValue) => {
    window.localStorage.setItem('auth-storage', JSON.stringify(storageValue));
    window.localStorage.removeItem('accessToken');
  }, unauthenticatedState);

  await mockTeamPageApis(page, { activeSeasons: [] });

  await page.goto('/teams');

  await expect(page.getByText('현재 진행 중인 시즌이 없습니다')).toBeVisible();
  await expect(page.getByRole('link', { name: '시즌 목록 보기' })).toBeVisible();
});

test('비로그인 사용자는 팀 목록은 보이지만 팀 만들기와 AI 추천 버튼은 보이지 않는다', async ({ page }) => {
  await page.addInitScript((storageValue) => {
    window.localStorage.setItem('auth-storage', JSON.stringify(storageValue));
    window.localStorage.removeItem('accessToken');
  }, unauthenticatedState);

  await mockTeamPageApis(page);

  await page.goto('/teams');

  await expect(page.getByRole('heading', { name: '팀 찾기' })).toBeVisible();
  await expect(page.getByText('알파 팀')).toBeVisible();
  await expect(page.getByRole('button', { name: '팀 만들기' })).not.toBeVisible();
  await expect(page.getByRole('button', { name: 'AI 추천' })).not.toBeVisible();
});

test('인증 완료 사용자는 팀 만들기와 AI 추천 버튼이 보인다', async ({ page }) => {
  await page.addInitScript((storageValue) => {
    window.localStorage.setItem('accessToken', 'fake-token');
    window.localStorage.setItem('auth-storage', JSON.stringify(storageValue));
  }, authenticatedState);

  await mockTeamPageApis(page);

  await page.goto('/teams');

  await expect(page.getByRole('button', { name: '팀 만들기' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'AI 추천' })).toBeVisible();
});

test('모집 중 필터와 역할 필터가 함께 동작한다', async ({ page }) => {
  await page.addInitScript((storageValue) => {
    window.localStorage.setItem('accessToken', 'fake-token');
    window.localStorage.setItem('auth-storage', JSON.stringify(storageValue));
  }, authenticatedState);

  await mockTeamPageApis(page, {
    teamsBySeason: [
      ...teams,
      {
        ...teams[0],
        id: 13,
        name: '감마 팀',
        status: 'IN_PROGRESS',
        recruitingFrontend: false,
        description: '진행 중이라 모집 안 함',
      },
    ],
    recruitingTeams: teams,
  });

  await page.goto('/teams');

  await page.getByRole('button', { name: '모집 중' }).click();
  await expect(page.getByText('감마 팀')).not.toBeVisible();

  await page.getByRole('button', { name: '백엔드' }).click();
  await expect(page.getByText('베타 팀')).toBeVisible();
  await expect(page.getByText('알파 팀')).not.toBeVisible();
});

test('인증 완료 사용자가 팀 생성 모달에서 이름 없이 제출하면 에러가 보인다', async ({ page }) => {
  await page.addInitScript((storageValue) => {
    window.localStorage.setItem('accessToken', 'fake-token');
    window.localStorage.setItem('auth-storage', JSON.stringify(storageValue));
  }, authenticatedState);

  await mockTeamPageApis(page);

  await page.goto('/teams');
  await page.getByRole('button', { name: '팀 만들기' }).first().click();
  await page.getByRole('button', { name: '팀 만들기' }).nth(1).click();

  await expect(page.getByText('팀 이름을 입력해주세요.')).toBeVisible();
});

test('인증 완료 사용자가 역할 없이 팀 생성 제출하면 에러가 보인다', async ({ page }) => {
  await page.addInitScript((storageValue) => {
    window.localStorage.setItem('accessToken', 'fake-token');
    window.localStorage.setItem('auth-storage', JSON.stringify(storageValue));
  }, authenticatedState);

  await mockTeamPageApis(page);

  await page.goto('/teams');
  await page.getByRole('button', { name: '팀 만들기' }).first().click();
  await page.getByPlaceholder('팀 이름을 입력하세요').fill('새 팀');
  await page.getByRole('button', { name: '팀 만들기' }).nth(1).click();

  await expect(page.getByText('본인의 역할을 선택해주세요.')).toBeVisible();
});

test('인증 완료 사용자가 팀 생성에 성공하면 생성된 팀 상세로 이동한다', async ({ page }) => {
  await page.addInitScript((storageValue) => {
    window.localStorage.setItem('accessToken', 'fake-token');
    window.localStorage.setItem('auth-storage', JSON.stringify(storageValue));
  }, authenticatedState);

  await mockTeamPageApis(page);

  await page.goto('/teams');
  await page.getByRole('button', { name: '팀 만들기' }).first().click();
  await page.getByPlaceholder('팀 이름을 입력하세요').fill('새 팀');
  await page.getByPlaceholder('팀의 목표나 프로젝트 아이디어를 소개해주세요').fill('새로 만든 팀 설명');
  await page.getByRole('radio', { name: '기획자' }).check();
  await page.getByRole('checkbox', { name: '프론트엔드' }).check();
  await page.getByRole('checkbox', { name: '백엔드' }).check();
  await page.getByRole('button', { name: '팀 만들기' }).nth(1).click();

  await expect(page).toHaveURL(/\/teams\/99$/);
  await expect(page.getByRole('heading', { name: '새 팀' })).toBeVisible();
});

test('AI 추천 성공 시 추천 팀 목록과 이유가 보인다', async ({ page }) => {
  await page.addInitScript((storageValue) => {
    window.localStorage.setItem('accessToken', 'fake-token');
    window.localStorage.setItem('auth-storage', JSON.stringify(storageValue));
  }, authenticatedState);

  await mockTeamPageApis(page);

  await page.goto('/teams');
  await page.getByRole('button', { name: 'AI 추천' }).click();

  const modal = page.locator('.fixed.inset-0').last();
  await expect(page.getByRole('heading', { name: 'AI 팀 추천' })).toBeVisible();
  await expect(modal.getByRole('heading', { name: '알파 팀' })).toBeVisible();
  await expect(modal.getByText('프론트엔드 역할과 잘 맞습니다.')).toBeVisible();
  await expect(modal.getByText('일정 충돌 위험: 낮음')).toBeVisible();
});

test('AI 추천 실패 시 프로필 먼저 등록 안내가 보인다', async ({ page }) => {
  await page.addInitScript((storageValue) => {
    window.localStorage.setItem('accessToken', 'fake-token');
    window.localStorage.setItem('auth-storage', JSON.stringify(storageValue));
  }, authenticatedState);

  await mockTeamPageApis(page, {
    teamRecommendationError: {
      errorCode: 'PROFILE_001',
      message: '프로필을 먼저 등록해주세요.',
      status: 400,
    },
  });

  await page.goto('/teams');
  await page.getByRole('button', { name: 'AI 추천' }).click();

  await expect(page.getByText('프로필을 먼저 등록해주세요. AI가 당신에게 맞는 팀을 추천해드립니다.')).toBeVisible();
});

test('AI 추천 실패 시 이미 팀 소속 안내가 보인다', async ({ page }) => {
  await page.addInitScript((storageValue) => {
    window.localStorage.setItem('accessToken', 'fake-token');
    window.localStorage.setItem('auth-storage', JSON.stringify(storageValue));
  }, authenticatedState);

  await mockTeamPageApis(page, {
    teamRecommendationError: {
      errorCode: 'TEAM_003',
      message: '이미 팀에 소속되어 있습니다.',
      status: 400,
    },
  });

  await page.goto('/teams');
  await page.getByRole('button', { name: 'AI 추천' }).click();

  await expect(page.getByText('이미 팀에 소속되어 있습니다.')).toBeVisible();
});

test('AI 추천 실패 시 추천 가능한 팀 없음 안내가 보인다', async ({ page }) => {
  await page.addInitScript((storageValue) => {
    window.localStorage.setItem('accessToken', 'fake-token');
    window.localStorage.setItem('auth-storage', JSON.stringify(storageValue));
  }, authenticatedState);

  await mockTeamPageApis(page, {
    teamRecommendationError: {
      errorCode: 'AI_002',
      message: '현재 추천 가능한 팀이 없습니다.',
      status: 400,
    },
  });

  await page.goto('/teams');
  await page.getByRole('button', { name: 'AI 추천' }).click();

  await expect(page.getByText('현재 추천 가능한 팀이 없습니다. 나중에 다시 시도해주세요.')).toBeVisible();
});
