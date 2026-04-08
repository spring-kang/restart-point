import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Eye, EyeOff, Mail, Lock, AlertCircle } from 'lucide-react';
import axios from 'axios';
import { useAuthStore } from '../stores/authStore';
import { authService } from '../services/authService';

// 에러 코드별 사용자 친화적 메시지 및 안내
const ERROR_MESSAGES: Record<string, { message: string; hint?: string }> = {
  USER_001: { message: '등록되지 않은 이메일입니다.', hint: '회원가입을 진행해주세요.' },
  USER_003: { message: '비밀번호가 일치하지 않습니다.', hint: '비밀번호를 다시 확인해주세요.' },
  USER_006: { message: '이메일 인증이 필요합니다.', hint: '가입 시 사용한 이메일의 인증을 완료해주세요.' },
};

export default function Login() {
  const navigate = useNavigate();
  const { setAuth } = useAuthStore();

  const [formData, setFormData] = useState({
    email: '',
    password: '',
  });
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<{ message: string; hint?: string; code?: string } | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
    setError(null);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError(null);

    try {
      const response = await authService.login(formData);
      setAuth(response.user, response.accessToken);
      navigate('/');
    } catch (err: unknown) {
      if (axios.isAxiosError(err) && err.response?.data) {
        const { errorCode, message } = err.response.data;
        const customError = errorCode ? ERROR_MESSAGES[errorCode] : null;

        setError({
          message: customError?.message || message || '로그인에 실패했습니다.',
          hint: customError?.hint,
          code: errorCode,
        });
      } else {
        setError({ message: '네트워크 오류가 발생했습니다. 잠시 후 다시 시도해주세요.' });
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-[calc(100vh-64px)] flex items-center justify-center py-12 px-4">
      <div className="w-full max-w-md">
        <div className="card">
          <div className="text-center mb-8">
            <h1 className="text-2xl font-bold text-neutral-900 mb-2">로그인</h1>
            <p className="text-neutral-600">Re:Start Point에 오신 것을 환영합니다</p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-5">
            {error && (
              <div className="bg-red-50 border border-red-200 px-4 py-3 rounded-xl">
                <div className="flex items-start gap-3">
                  <AlertCircle className="w-5 h-5 text-red-500 flex-shrink-0 mt-0.5" />
                  <div className="flex-1">
                    <p className="text-red-700 font-medium text-sm">{error.message}</p>
                    {error.hint && (
                      <p className="text-red-600 text-sm mt-1">{error.hint}</p>
                    )}
                    {error.code === 'USER_001' && (
                      <Link to="/signup" className="text-primary-600 text-sm font-medium hover:underline mt-2 inline-block">
                        회원가입 하러 가기 &rarr;
                      </Link>
                    )}
                  </div>
                </div>
              </div>
            )}

            <div>
              <label htmlFor="email" className="label">이메일</label>
              <div className="relative">
                <Mail className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-neutral-400" />
                <input
                  type="email"
                  id="email"
                  name="email"
                  value={formData.email}
                  onChange={handleChange}
                  className="input-field pl-12"
                  placeholder="example@email.com"
                  required
                />
              </div>
            </div>

            <div>
              <label htmlFor="password" className="label">비밀번호</label>
              <div className="relative">
                <Lock className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-neutral-400" />
                <input
                  type={showPassword ? 'text' : 'password'}
                  id="password"
                  name="password"
                  value={formData.password}
                  onChange={handleChange}
                  className="input-field pl-12 pr-12"
                  placeholder="비밀번호를 입력하세요"
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-4 top-1/2 -translate-y-1/2 text-neutral-400 hover:text-neutral-600"
                >
                  {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                </button>
              </div>
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="btn-primary w-full disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isLoading ? '로그인 중...' : '로그인'}
            </button>
          </form>

          <div className="mt-6 text-center">
            <p className="text-neutral-600 text-sm">
              계정이 없으신가요?{' '}
              <Link to="/signup" className="text-primary-600 font-medium hover:underline">
                회원가입
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
