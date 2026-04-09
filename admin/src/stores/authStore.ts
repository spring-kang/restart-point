import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { User } from '../types';
import api from '../services/api';

interface AuthState {
  user: User | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
  checkAuth: () => Promise<void>;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      accessToken: null,
      isAuthenticated: false,
      isLoading: true,

      login: async (email: string, password: string) => {
        const response = await api.post('/auth/login', { email, password });
        const { accessToken, user } = response.data.data;

        // ADMIN 권한 체크
        if (user.role !== 'ADMIN') {
          throw new Error('관리자 권한이 필요합니다.');
        }

        localStorage.setItem('accessToken', accessToken);
        set({ user, accessToken, isAuthenticated: true });
      },

      logout: () => {
        localStorage.removeItem('accessToken');
        set({ user: null, accessToken: null, isAuthenticated: false });
      },

      checkAuth: async () => {
        const token = localStorage.getItem('accessToken');
        if (!token) {
          set({ isLoading: false, isAuthenticated: false });
          return;
        }

        try {
          const response = await api.get('/users/me');
          const user = response.data.data;

          // ADMIN 권한 체크
          if (user.role !== 'ADMIN') {
            localStorage.removeItem('accessToken');
            set({ user: null, accessToken: null, isAuthenticated: false, isLoading: false });
            return;
          }

          set({ user, accessToken: token, isAuthenticated: true, isLoading: false });
        } catch {
          localStorage.removeItem('accessToken');
          set({ user: null, accessToken: null, isAuthenticated: false, isLoading: false });
        }
      },
    }),
    {
      name: 'admin-auth-storage',
      partialize: (state) => ({ accessToken: state.accessToken }),
    }
  )
);
