import React from 'react';
import type { ReactNode } from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import Login from './Login';
import { createUser } from '../test/testData';

const { navigateMock, loginMock, setAuthMock } = vi.hoisted(() => ({
  navigateMock: vi.fn(),
  loginMock: vi.fn(),
  setAuthMock: vi.fn(),
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    Link: ({ children, to, ...props }: { children: ReactNode; to: string }) => (
      <a href={to} {...props}>{children}</a>
    ),
    useNavigate: () => navigateMock,
  };
});

vi.mock('../services/authService', () => ({
  authService: {
    login: loginMock,
  },
}));

vi.mock('../stores/authStore', () => ({
  useAuthStore: () => ({
    setAuth: setAuthMock,
  }),
}));

describe('Login', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('로그인 성공 시 인증 정보를 저장하고 홈으로 이동한다', async () => {
    const user = userEvent.setup();
    loginMock.mockResolvedValue({
      accessToken: 'access-token',
      user: createUser(),
    });

    render(<Login />);

    await user.type(screen.getByLabelText('이메일'), 'test@example.com');
    await user.type(screen.getByLabelText('비밀번호'), 'password123');
    await user.click(screen.getByRole('button', { name: '로그인' }));

    await waitFor(() => {
      expect(loginMock).toHaveBeenCalledWith({
        email: 'test@example.com',
        password: 'password123',
      });
      expect(setAuthMock).toHaveBeenCalledWith(createUser(), 'access-token');
      expect(navigateMock).toHaveBeenCalledWith('/');
    });
  });

  it('로그인 실패 시 에러 메시지를 보여준다', async () => {
    const user = userEvent.setup();
    loginMock.mockRejectedValue({
      response: {
        data: {
          message: '로그인에 실패했습니다.',
        },
      },
    });

    render(<Login />);

    await user.type(screen.getByLabelText('이메일'), 'test@example.com');
    await user.type(screen.getByLabelText('비밀번호'), 'wrong-password');
    await user.click(screen.getByRole('button', { name: '로그인' }));

    expect(await screen.findByText('로그인에 실패했습니다.')).toBeInTheDocument();
    expect(setAuthMock).not.toHaveBeenCalled();
    expect(navigateMock).not.toHaveBeenCalled();
  });
});
