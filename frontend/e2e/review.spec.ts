import { test, expect } from '@playwright/test';

const approvedReviewerState = {
  state: {
    user: {
      id: 1,
      email: 'reviewer@restart-point.com',
      name: '리뷰어',
      role: 'REVIEWER',
      emailVerified: true,
      certificationStatus: 'APPROVED',
      createdAt: '2026-04-10T00:00:00Z',
      updatedAt: '2026-04-10T00:00:00Z',
    },
    accessToken: 'fake-token',
    isAuthenticated: true,
  },
  version: 0,
};

const reviewingSeason = {
  id: 1,
  title: '테스트 시즌',
  description: '심사 테스트용 시즌',
  status: 'REVIEWING',
  recruitmentStartAt: '2026-04-01T00:00:00',
  recruitmentEndAt: '2026-04-05T00:00:00',
  teamBuildingStartAt: '2026-04-06T00:00:00',
  teamBuildingEndAt: '2026-04-07T00:00:00',
  projectStartAt: '2026-04-08T00:00:00',
  projectEndAt: '2026-04-15T00:00:00',
  reviewStartAt: '2026-04-16T00:00:00',
  reviewEndAt: '2026-04-20T00:00:00',
  expertReviewWeight: 70,
  candidateReviewWeight: 30,
  currentPhase: '심사 진행 중',
  canJoin: false,
};

const reviewableProjects = [
  {
    id: 101,
    teamId: 11,
    teamName: '알파팀',
    name: 'AI 회고 도우미',
    problemDefinition: '팀 회고가 산발적으로 흩어지는 문제',
    targetUsers: '프로젝트 팀원',
    solution: '회고를 구조화해서 정리하는 서비스',
    aiUsage: '요약 및 액션 아이템 추출',
    githubUrl: 'https://github.com/example/repo',
    demoUrl: 'https://demo.example.com',
    status: 'SUBMITTED',
    createdAt: '2026-04-10T00:00:00Z',
  },
];

const myReviews = [
  {
    id: 500,
    projectId: 99,
    projectName: '이전 심사 프로젝트',
    reviewerId: 1,
    reviewerName: '리뷰어',
    averageScore: 4.2,
    overallComment: '좋은 시도였습니다.',
    submittedAt: '2026-04-09T00:00:00Z',
  },
];

test.beforeEach(async ({ page }) => {
  await page.addInitScript((storageValue) => {
    window.localStorage.setItem('accessToken', 'fake-token');
    window.localStorage.setItem('auth-storage', JSON.stringify(storageValue));
  }, approvedReviewerState);
});

test('심사 중 시즌 상세에서 프로젝트 심사하기 버튼이 보인다', async ({ page }) => {
  await page.route('**/api/v1/seasons/1', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: reviewingSeason }),
    });
  });

  await page.goto('/seasons/1');

  await expect(page.getByRole('heading', { name: '테스트 시즌' })).toBeVisible();
  await expect(page.getByRole('link', { name: '프로젝트 심사하기' })).toBeVisible();
});

test('심사 페이지에서 점수 없이 제출하면 검증 에러가 보인다', async ({ page }) => {
  await page.route('**/api/v1/seasons/1/reviewable-projects', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: reviewableProjects }),
    });
  });

  await page.route('**/api/v1/users/me/reviews', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: myReviews }),
    });
  });

  await page.goto('/seasons/1/review');

  await expect(page.getByRole('heading', { name: '프로젝트 심사' })).toBeVisible();
  await expect(page.getByText('완료된 심사')).toBeVisible();

  await page.getByRole('button', { name: '심사하기' }).click();
  await expect(page.getByText('프로젝트 정보')).toBeVisible();

  await page.getByRole('button', { name: '심사 제출' }).click();
  await expect(page.getByText('모든 항목에 점수를 입력해주세요.')).toBeVisible();
});

test('심사 페이지에서 점수 입력 후 제출하면 목록이 새로고침된다', async ({ page }) => {
  let reviewSubmitCount = 0;

  await page.route('**/api/v1/seasons/1/reviewable-projects', async (route) => {
    const body = reviewSubmitCount === 0 ? reviewableProjects : [];
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: body }),
    });
  });

  await page.route('**/api/v1/users/me/reviews', async (route) => {
    const body = reviewSubmitCount === 0 ? myReviews : [...myReviews, {
      id: 501,
      projectId: 101,
      projectName: 'AI 회고 도우미',
      reviewerId: 1,
      reviewerName: '리뷰어',
      averageScore: 4.5,
      overallComment: '전반적으로 우수합니다.',
      submittedAt: '2026-04-10T00:00:00Z',
    }];

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: body }),
    });
  });

  await page.route('**/api/v1/projects/101/reviews', async (route) => {
    reviewSubmitCount += 1;
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          id: 900,
          projectId: 101,
          projectName: 'AI 회고 도우미',
          reviewerId: 1,
          reviewerName: '리뷰어',
          averageScore: 4.5,
          overallComment: '전반적으로 우수합니다.',
          submittedAt: '2026-04-10T00:00:00Z',
        },
      }),
    });
  });

  await page.goto('/seasons/1/review');
  await page.getByRole('button', { name: '심사하기' }).click();

  for (const label of ['문제 정의의 명확성', '사용자 가치', 'AI 활용 적절성', 'UX 완성도', '기술 구현 가능성', '협업 완성도']) {
    await page
      .locator('div.border.border-gray-200.rounded-lg.p-4')
      .filter({ has: page.getByRole('heading', { name: label }) })
      .getByRole('button', { name: '4 우수' })
      .click();
  }

  await page.getByPlaceholder('프로젝트에 대한 전체적인 피드백을 작성해주세요.').fill('전반적으로 우수합니다.');
  await page.getByRole('button', { name: '심사 제출' }).click();

  await expect(page.getByText('심사할 프로젝트가 없습니다.')).toBeVisible();
  await expect(page.getByText('AI 회고 도우미')).toBeVisible();
});
