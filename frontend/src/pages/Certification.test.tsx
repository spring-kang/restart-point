import React from 'react';
import type { User } from '../types';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import Certification from './Certification';
import { createUser } from '../test/testData';

const { navigateMock, getMeMock, updateUserMock, authState } = vi.hoisted(() => ({
  navigateMock: vi.fn(),
  getMeMock: vi.fn(),
  updateUserMock: vi.fn(),
  authState: {
    user: {
      id: 1,
      email: 'test@example.com',
      name: '테스터',
      role: 'USER',
      certificationStatus: 'NONE',
      createdAt: '2026-04-07T00:00:00',
      updatedAt: '2026-04-07T00:00:00',
    } as User | null,
    isAuthenticated: true,
  },
}));

vi.mock('react-router-dom', () => ({
  useNavigate: () => navigateMock,
}));

vi.mock('../services/authService', () => ({
  authService: {
    getMe: getMeMock,
    requestCertification: vi.fn(),
  },
}));

vi.mock('../stores/authStore', () => ({
  useAuthStore: () => ({
    user: authState.user,
    isAuthenticated: authState.isAuthenticated,
    updateUser: updateUserMock,
  }),
}));

describe('Certification', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    authState.user = createUser();
    authState.isAuthenticated = true;
  });

  it('비로그인 상태면 로그인 페이지로 리다이렉트한다', async () => {
    authState.user = null;
    authState.isAuthenticated = false;

    const { container } = render(<Certification />);

    await waitFor(() => {
      expect(navigateMock).toHaveBeenCalledWith('/login', { replace: true });
    });
    expect(container).toBeEmptyDOMElement();
  });

  it('인증된 사용자는 진입 시 최신 사용자 상태를 조회하고 수동 새로고침도 가능하다', async () => {
    authState.user = createUser({
      certificationStatus: 'PENDING',
      bootcampName: '코드잇 스프린트',
      bootcampGeneration: '1기',
    });
    const freshUser = createUser({
      certificationStatus: 'PENDING',
      bootcampName: '코드잇 스프린트',
      bootcampGeneration: '1기',
    });
    getMeMock.mockResolvedValue(freshUser);
    const user = userEvent.setup();

    render(<Certification />);

    await waitFor(() => {
      expect(getMeMock).toHaveBeenCalledTimes(1);
      expect(updateUserMock).toHaveBeenCalledWith(freshUser);
    });

    await user.click(screen.getByRole('button', { name: '상태 새로고침' }));

    await waitFor(() => {
      expect(getMeMock).toHaveBeenCalledTimes(2);
    });
  });
});
