import { Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../stores/authStore';
import { Menu, X, User, LogOut, ChevronDown, Bell } from 'lucide-react';
import { useState } from 'react';
import NotificationDropdown from '../notification/NotificationDropdown';

export default function Header() {
  const { user, isAuthenticated, logout } = useAuthStore();
  const navigate = useNavigate();
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [isProfileMenuOpen, setIsProfileMenuOpen] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  const getCertificationBadge = () => {
    if (!user) return null;

    switch (user.certificationStatus) {
      case 'APPROVED':
        return <span className="badge-primary text-xs">인증완료</span>;
      case 'PENDING':
        return <span className="badge bg-yellow-100 text-yellow-700 text-xs">인증대기</span>;
      default:
        return <span className="badge-neutral text-xs">미인증</span>;
    }
  };

  return (
    <header className="bg-white border-b border-neutral-100 sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16">
          {/* 로고 */}
          <Link to="/" className="flex items-center gap-2">
            <div className="w-8 h-8 bg-gradient-to-br from-primary-500 to-primary-600 rounded-lg flex items-center justify-center">
              <span className="text-white font-bold text-sm">R</span>
            </div>
            <span className="font-semibold text-lg text-neutral-900">Re:Start Point</span>
          </Link>

          {/* 데스크톱 네비게이션 */}
          <nav className="hidden md:flex items-center gap-8">
            <Link to="/seasons" className="text-neutral-600 hover:text-primary-600 transition-colors">
              시즌
            </Link>
            <Link to="/teams" className="text-neutral-600 hover:text-primary-600 transition-colors">
              팀 탐색
            </Link>
            {isAuthenticated && (
              <Link to="/my-team" className="text-neutral-600 hover:text-primary-600 transition-colors">
                내 팀
              </Link>
            )}
          </nav>

          {/* 데스크톱 우측 메뉴 */}
          <div className="hidden md:flex items-center gap-4">
            {isAuthenticated ? (
              <>
                <NotificationDropdown />
                <div className="relative">
                <button
                  onClick={() => setIsProfileMenuOpen(!isProfileMenuOpen)}
                  className="flex items-center gap-2 px-3 py-2 rounded-xl hover:bg-neutral-50 transition-colors"
                >
                  <div className="w-8 h-8 bg-primary-100 rounded-full flex items-center justify-center">
                    <User className="w-4 h-4 text-primary-600" />
                  </div>
                  <span className="text-sm font-medium text-neutral-700">{user?.name}</span>
                  {getCertificationBadge()}
                  <ChevronDown className="w-4 h-4 text-neutral-400" />
                </button>

                {isProfileMenuOpen && (
                  <div className="absolute right-0 mt-2 w-48 bg-white rounded-xl shadow-elevated border border-neutral-100 py-2">
                    <Link
                      to="/profile"
                      className="flex items-center gap-2 px-4 py-2 text-sm text-neutral-700 hover:bg-neutral-50"
                      onClick={() => setIsProfileMenuOpen(false)}
                    >
                      <User className="w-4 h-4" />
                      프로필
                    </Link>
                    {user?.certificationStatus !== 'APPROVED' && (
                      <Link
                        to="/certification"
                        className="flex items-center gap-2 px-4 py-2 text-sm text-neutral-700 hover:bg-neutral-50"
                        onClick={() => setIsProfileMenuOpen(false)}
                      >
                        수료 인증
                      </Link>
                    )}
                    <hr className="my-2 border-neutral-100" />
                    <button
                      onClick={handleLogout}
                      className="flex items-center gap-2 px-4 py-2 text-sm text-red-600 hover:bg-red-50 w-full"
                    >
                      <LogOut className="w-4 h-4" />
                      로그아웃
                    </button>
                  </div>
                )}
              </div>
              </>
            ) : (
              <>
                <Link to="/login" className="btn-secondary text-sm">
                  로그인
                </Link>
                <Link to="/signup" className="btn-primary text-sm">
                  회원가입
                </Link>
              </>
            )}
          </div>

          {/* 모바일 메뉴 버튼 */}
          <button
            className="md:hidden p-2"
            onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
          >
            {isMobileMenuOpen ? (
              <X className="w-6 h-6 text-neutral-600" />
            ) : (
              <Menu className="w-6 h-6 text-neutral-600" />
            )}
          </button>
        </div>

        {/* 모바일 메뉴 */}
        {isMobileMenuOpen && (
          <div className="md:hidden py-4 border-t border-neutral-100">
            <nav className="flex flex-col gap-2">
              <Link
                to="/seasons"
                className="px-4 py-2 text-neutral-600 hover:bg-neutral-50 rounded-lg"
                onClick={() => setIsMobileMenuOpen(false)}
              >
                시즌
              </Link>
              <Link
                to="/teams"
                className="px-4 py-2 text-neutral-600 hover:bg-neutral-50 rounded-lg"
                onClick={() => setIsMobileMenuOpen(false)}
              >
                팀 탐색
              </Link>
              {isAuthenticated ? (
                <>
                  <Link
                    to="/my-team"
                    className="px-4 py-2 text-neutral-600 hover:bg-neutral-50 rounded-lg"
                    onClick={() => setIsMobileMenuOpen(false)}
                  >
                    내 팀
                  </Link>
                  <Link
                    to="/notifications"
                    className="flex items-center gap-2 px-4 py-2 text-neutral-600 hover:bg-neutral-50 rounded-lg"
                    onClick={() => setIsMobileMenuOpen(false)}
                  >
                    <Bell className="w-4 h-4" />
                    알림
                  </Link>
                  <Link
                    to="/profile"
                    className="px-4 py-2 text-neutral-600 hover:bg-neutral-50 rounded-lg"
                    onClick={() => setIsMobileMenuOpen(false)}
                  >
                    프로필
                  </Link>
                  <button
                    onClick={() => {
                      handleLogout();
                      setIsMobileMenuOpen(false);
                    }}
                    className="px-4 py-2 text-red-600 hover:bg-red-50 rounded-lg text-left"
                  >
                    로그아웃
                  </button>
                </>
              ) : (
                <div className="flex gap-2 px-4 pt-2">
                  <Link to="/login" className="btn-secondary flex-1 text-center text-sm">
                    로그인
                  </Link>
                  <Link to="/signup" className="btn-primary flex-1 text-center text-sm">
                    회원가입
                  </Link>
                </div>
              )}
            </nav>
          </div>
        )}
      </div>
    </header>
  );
}
