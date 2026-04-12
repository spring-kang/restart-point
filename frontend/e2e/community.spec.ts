import { test, expect } from '@playwright/test';

const normalUser = {
  id: 21,
  email: 'test@example.com',
  name: '커뮤니티유저',
  role: 'USER',
  emailVerified: true,
  certificationStatus: 'APPROVED',
  createdAt: '2026-04-12T00:00:00Z',
  updatedAt: '2026-04-12T00:00:00Z',
};

const adminUser = {
  ...normalUser,
  id: 22,
  email: 'admin@restart-point.com',
  name: '관리자',
  role: 'ADMIN',
};

const unauthenticatedState = {
  state: { user: null, accessToken: null, isAuthenticated: false },
  version: 0,
};

const userState = {
  state: { user: normalUser, accessToken: 'fake-token', isAuthenticated: true },
  version: 0,
};

const adminState = {
  state: { user: adminUser, accessToken: 'fake-token', isAuthenticated: true },
  version: 0,
};

const seasonPage = {
  content: [
    { id: 1, title: '2026 봄 시즌', status: 'RECRUITING' },
  ],
  totalPages: 1,
  totalElements: 1,
  size: 20,
  number: 0,
};

const postsPage = {
  content: [
    {
      id: 101,
      title: '팀원 모집합니다',
      postType: 'RECRUITMENT',
      pinned: false,
      viewCount: 12,
      likeCount: 3,
      commentCount: 2,
      createdAt: '2026-04-12T00:00:00Z',
      author: { id: 21, name: '커뮤니티유저' },
    },
    {
      id: 102,
      title: '공지 샘플',
      postType: 'ANNOUNCEMENT',
      pinned: true,
      viewCount: 40,
      likeCount: 5,
      commentCount: 0,
      createdAt: '2026-04-12T00:00:00Z',
      author: { id: 22, name: '관리자' },
    },
  ],
  totalPages: 1,
  totalElements: 2,
  size: 20,
  number: 0,
};

const postDetail = {
  id: 101,
  title: '팀원 모집합니다',
  content: '함께할 프론트엔드 팀원을 찾고 있어요.',
  postType: 'RECRUITMENT',
  pinned: false,
  liked: false,
  likeCount: 3,
  commentCount: 1,
  viewCount: 12,
  createdAt: '2026-04-12T00:00:00Z',
  author: { id: 21, name: '커뮤니티유저' },
  season: { id: 1, title: '2026 봄 시즌' },
};

const comments = [
  {
    id: 500,
    content: '응원합니다!',
    deleted: false,
    createdAt: '2026-04-12T00:00:00Z',
    author: { id: 30, name: '댓글러' },
    replies: [],
  },
];

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

async function mockCommunityListApis(page: import('@playwright/test').Page) {
  await page.route('**/api/v1/seasons/active', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: seasonPage.content }) });
  });

  await page.route('**/api/v1/community/posts**', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: postsPage }) });
  });

  await page.route('**/api/v1/community/recruitment**', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: { ...postsPage, content: [postsPage.content[0]] } }) });
  });

  await page.route('**/api/v1/community/announcements**', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: { ...postsPage, content: [postsPage.content[1]] } }) });
  });

  await page.route('**/api/v1/community/showcase**', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: { ...postsPage, content: [] } }) });
  });

  await page.route('**/api/v1/community/qna**', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: { ...postsPage, content: [] } }) });
  });
}

async function mockPostDetailApis(page: import('@playwright/test').Page) {
  await page.route('**/api/v1/community/posts/101/comments', async (route) => {
    if (route.request().method() === 'GET') {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: comments }) });
      return;
    }

    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: { id: 999 } }) });
  });

  await page.route('**/api/v1/community/posts/101/like', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: true }) });
  });

  await page.route('**/api/v1/community/posts/101', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: postDetail }) });
  });
}

async function mockWriteApis(page: import('@playwright/test').Page) {
  await page.route('**/api/v1/seasons/active', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: seasonPage.content }) });
  });

  await page.route('**/api/v1/community/posts', async (route) => {
    if (route.request().method() === 'POST') {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: { id: 777 } }) });
      return;
    }

    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: postsPage }) });
  });
}

test('비로그인 사용자는 커뮤니티 목록은 볼 수 있지만 글쓰기 버튼은 보이지 않는다', async ({ page }) => {
  await setAuth(page, unauthenticatedState);
  await mockCommunityListApis(page);

  await page.goto('/community');

  await expect(page.getByRole('heading', { name: '커뮤니티' })).toBeVisible();
  await expect(page.getByRole('link', { name: '글쓰기' })).not.toBeVisible();
  await expect(page.getByRole('link', { name: /팀원 모집합니다/ })).toBeVisible();
});

test('로그인 사용자는 글쓰기 버튼과 탭 필터를 사용할 수 있다', async ({ page }) => {
  await setAuth(page, userState, true);
  await mockCommunityListApis(page);

  await page.goto('/community');
  await expect(page.getByRole('link', { name: '글쓰기' })).toBeVisible();

  await page.getByRole('button', { name: '공지사항' }).click();
  await expect(page).toHaveURL(/type=ANNOUNCEMENT/);
  await expect(page.getByRole('link', { name: /공지 샘플/ })).toBeVisible();
});

test('게시글 상세에서 비로그인 사용자가 좋아요를 누르면 로그인 페이지로 이동한다', async ({ page }) => {
  await setAuth(page, unauthenticatedState);
  await mockPostDetailApis(page);

  await page.goto('/community/posts/101');
  await page.getByRole('button', { name: /좋아요 3/ }).click();

  await expect(page).toHaveURL(/\/login$/);
});

test('로그인 사용자는 댓글을 작성할 수 있다', async ({ page }) => {
  await setAuth(page, userState, true);
  await mockPostDetailApis(page);

  await page.goto('/community/posts/101');
  await page.getByPlaceholder('댓글을 입력하세요').fill('좋은 모집 글이네요');
  await page.getByRole('button', { name: '작성' }).click();

  await expect(page.getByText('댓글 2개')).toBeVisible();
});

test('글쓰기 페이지는 비로그인 사용자를 로그인 페이지로 보낸다', async ({ page }) => {
  await setAuth(page, unauthenticatedState);
  await page.goto('/community/write');
  await expect(page).toHaveURL(/\/login$/);
});

test('일반 사용자는 글쓰기에서 공지사항 옵션이 숨겨진다', async ({ page }) => {
  await setAuth(page, userState, true);
  await mockWriteApis(page);

  await page.goto('/community/write');

  await expect(page.getByRole('button', { name: '공지사항' })).not.toBeVisible();
  await expect(page.getByRole('button', { name: '팀원 모집' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Q&A' })).toBeVisible();
});

test('관리자는 공지사항 글을 작성하면 상세 페이지로 이동한다', async ({ page }) => {
  await setAuth(page, adminState, true);
  await mockWriteApis(page);

  await page.goto('/community/write');
  await page.getByRole('button', { name: '공지사항' }).click();
  await page.getByPlaceholder('제목을 입력하세요').fill('관리자 공지');
  await page.getByPlaceholder('내용을 입력하세요.').fill('중요 공지입니다.');
  await page.getByRole('button', { name: '등록하기' }).click();

  await expect(page).toHaveURL(/\/community\/posts\/777$/);
});
