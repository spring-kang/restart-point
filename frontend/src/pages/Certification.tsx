import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Award, Building2, Calendar, Link as LinkIcon, CheckCircle, Clock, XCircle, RefreshCw } from 'lucide-react';
import { useAuthStore } from '../stores/authStore';
import { authService } from '../services/authService';

export default function Certification() {
  const navigate = useNavigate();
  const { user, updateUser, isAuthenticated } = useAuthStore();

  const [formData, setFormData] = useState({
    bootcampName: '',
    bootcampGeneration: '',
    graduationDate: '',
    certificateUrl: '',
  });
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [isRefreshing, setIsRefreshing] = useState(false);

  // 비로그인 사용자 리다이렉트
  useEffect(() => {
    if (!isAuthenticated) {
      navigate('/login', { replace: true });
    }
  }, [isAuthenticated, navigate]);

  // 페이지 진입 시 최신 사용자 정보 조회
  useEffect(() => {
    const refreshUserStatus = async () => {
      if (!isAuthenticated) return;
      try {
        const freshUser = await authService.getMe();
        updateUser(freshUser);
      } catch (err) {
        console.error('Failed to refresh user status:', err);
      }
    };
    refreshUserStatus();
  }, [isAuthenticated, updateUser]);

  // 수동 새로고침 함수
  const handleRefresh = async () => {
    setIsRefreshing(true);
    try {
      const freshUser = await authService.getMe();
      updateUser(freshUser);
    } catch (err) {
      console.error('Failed to refresh user status:', err);
    } finally {
      setIsRefreshing(false);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
    setError('');
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError('');

    try {
      const updatedUser = await authService.requestCertification(formData);
      updateUser(updatedUser);
      navigate('/');
    } catch (err: unknown) {
      const errorMessage = err instanceof Error && 'response' in err
        ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
        : null;
      setError(errorMessage || '수료 인증 요청에 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  const renderStatus = () => {
    if (!user) return null;

    switch (user.certificationStatus) {
      case 'APPROVED':
        return (
          <div className="card bg-green-50 border border-green-200">
            <div className="flex items-center gap-3">
              <CheckCircle className="w-8 h-8 text-green-500" />
              <div>
                <h3 className="font-semibold text-green-800">인증 완료</h3>
                <p className="text-sm text-green-600">
                  {user.bootcampName} {user.bootcampGeneration} 수료가 확인되었습니다.
                </p>
              </div>
            </div>
          </div>
        );
      case 'PENDING':
        return (
          <div className="card bg-yellow-50 border border-yellow-200">
            <div className="flex items-center gap-3">
              <Clock className="w-8 h-8 text-yellow-500" />
              <div>
                <h3 className="font-semibold text-yellow-800">인증 대기 중</h3>
                <p className="text-sm text-yellow-600">
                  운영자가 수료 인증을 검토 중입니다. 잠시만 기다려주세요.
                </p>
              </div>
            </div>
          </div>
        );
      case 'REJECTED':
        return (
          <div className="card bg-red-50 border border-red-200 mb-6">
            <div className="flex items-center gap-3">
              <XCircle className="w-8 h-8 text-red-500" />
              <div>
                <h3 className="font-semibold text-red-800">인증 거절됨</h3>
                <p className="text-sm text-red-600">
                  수료 인증이 거절되었습니다. 정확한 정보로 다시 신청해주세요.
                </p>
              </div>
            </div>
          </div>
        );
      default:
        return null;
    }
  };

  // 비로그인 상태면 렌더링하지 않음
  if (!isAuthenticated) {
    return null;
  }

  // 이미 인증 완료 또는 대기 중인 경우
  if (user?.certificationStatus === 'APPROVED' || user?.certificationStatus === 'PENDING') {
    return (
      <div className="max-w-2xl mx-auto px-4 py-12">
        <div className="text-center mb-8">
          <Award className="w-16 h-16 text-primary-500 mx-auto mb-4" />
          <h1 className="text-2xl font-bold text-neutral-900 mb-2">수료 인증</h1>
        </div>
        {renderStatus()}
        <div className="flex justify-center gap-4 mt-6">
          <button
            onClick={handleRefresh}
            disabled={isRefreshing}
            className="btn-secondary flex items-center gap-2 disabled:opacity-50"
          >
            <RefreshCw className={`w-4 h-4 ${isRefreshing ? 'animate-spin' : ''}`} />
            {isRefreshing ? '확인 중...' : '상태 새로고침'}
          </button>
          <button onClick={() => navigate('/')} className="btn-secondary">
            홈으로 돌아가기
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto px-4 py-12">
      <div className="text-center mb-8">
        <Award className="w-16 h-16 text-primary-500 mx-auto mb-4" />
        <h1 className="text-2xl font-bold text-neutral-900 mb-2">수료 인증</h1>
        <p className="text-neutral-600">
          부트캠프 수료 정보를 입력해주세요. 인증 후 팀 참가가 가능합니다.
        </p>
      </div>

      {renderStatus()}

      <div className="card">
        <form onSubmit={handleSubmit} className="space-y-5">
          {error && (
            <div className="bg-red-50 text-red-600 px-4 py-3 rounded-xl text-sm">
              {error}
            </div>
          )}

          <div>
            <label htmlFor="bootcampName" className="label">부트캠프명</label>
            <div className="relative">
              <Building2 className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-neutral-400" />
              <input
                type="text"
                id="bootcampName"
                name="bootcampName"
                value={formData.bootcampName}
                onChange={handleChange}
                className="input-field pl-12"
                placeholder="예: 코드잇 스프린트"
                required
              />
            </div>
          </div>

          <div>
            <label htmlFor="bootcampGeneration" className="label">기수</label>
            <div className="relative">
              <Award className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-neutral-400" />
              <input
                type="text"
                id="bootcampGeneration"
                name="bootcampGeneration"
                value={formData.bootcampGeneration}
                onChange={handleChange}
                className="input-field pl-12"
                placeholder="예: 1기"
                required
              />
            </div>
          </div>

          <div>
            <label htmlFor="graduationDate" className="label">수료일</label>
            <div className="relative">
              <Calendar className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-neutral-400" />
              <input
                type="date"
                id="graduationDate"
                name="graduationDate"
                value={formData.graduationDate}
                onChange={handleChange}
                className="input-field pl-12"
                required
              />
            </div>
          </div>

          <div>
            <label htmlFor="certificateUrl" className="label">수료증 URL</label>
            <div className="relative">
              <LinkIcon className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-neutral-400" />
              <input
                type="url"
                id="certificateUrl"
                name="certificateUrl"
                value={formData.certificateUrl}
                onChange={handleChange}
                className="input-field pl-12"
                placeholder="수료증 이미지 또는 문서 URL"
                required
              />
            </div>
            <p className="mt-1.5 text-xs text-neutral-500">
              Google Drive, Dropbox 등에 업로드한 수료증 링크를 입력해주세요.
            </p>
          </div>

          <button
            type="submit"
            disabled={isLoading}
            className="btn-primary w-full disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isLoading ? '요청 중...' : '수료 인증 요청'}
          </button>
        </form>
      </div>
    </div>
  );
}
