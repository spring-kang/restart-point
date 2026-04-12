import { useEffect } from 'react';
import { Outlet } from 'react-router-dom';
import Header from './Header';
import { useAuthStore } from '../../stores/authStore';

export default function Layout() {
  const { isAuthenticated, refreshUser } = useAuthStore();

  // 앱 초기화 시 토큰 유효성 검증
  useEffect(() => {
    if (isAuthenticated) {
      refreshUser();
    }
  }, []); // 최초 마운트 시에만 실행

  return (
    <div className="min-h-screen bg-background">
      <Header />
      <main>
        <Outlet />
      </main>
      <footer className="bg-white border-t border-neutral-100 mt-auto">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <div className="flex flex-col md:flex-row justify-between items-center gap-4">
            <div className="flex items-center gap-2">
              <div className="w-6 h-6 bg-gradient-to-br from-primary-500 to-primary-600 rounded-md flex items-center justify-center">
                <span className="text-white font-bold text-xs">R</span>
              </div>
              <span className="text-sm text-neutral-600">Re:Start Point</span>
            </div>
            <p className="text-sm text-neutral-500">
              부트캠프 수료 이후의 성장을 다시 시작하는 AI 프로젝트 러닝 플랫폼
            </p>
            <p className="text-xs text-neutral-400">
              © {new Date().getFullYear()} Re:Start Point. All rights reserved.
            </p>
          </div>
        </div>
      </footer>
    </div>
  );
}
