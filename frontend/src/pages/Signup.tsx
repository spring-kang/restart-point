import { useState, useRef, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Eye, EyeOff, Mail, Lock, User, ArrowLeft, CheckCircle, RefreshCw } from 'lucide-react';
import axios from 'axios';
import { useAuthStore } from '../stores/authStore';
import { authService } from '../services/authService';

type Step = 'email' | 'verify' | 'signup';

export default function Signup() {
  const navigate = useNavigate();
  const { isAuthenticated, setAuth } = useAuthStore();

  // 이미 로그인된 상태면 메인 페이지로 리다이렉트
  useEffect(() => {
    if (isAuthenticated) {
      navigate('/', { replace: true });
    }
  }, [isAuthenticated, navigate]);

  const [step, setStep] = useState<Step>('email');
  const [email, setEmail] = useState('');
  const [code, setCode] = useState(['', '', '', '', '', '']);
  const [formData, setFormData] = useState({
    password: '',
    passwordConfirm: '',
    name: '',
  });
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [countdown, setCountdown] = useState(0);
  const [signupToken, setSignupToken] = useState('');
  const inputRefs = useRef<(HTMLInputElement | null)[]>([]);

  // 재발송 카운트다운
  useEffect(() => {
    if (countdown > 0) {
      const timer = setTimeout(() => setCountdown(countdown - 1), 1000);
      return () => clearTimeout(timer);
    }
  }, [countdown]);

  // Step 1: 이메일 입력 후 인증 코드 발송
  const handleEmailSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError('');

    try {
      await authService.sendVerificationCode(email);
      setStep('verify');
      setCountdown(60);
      setSignupToken('');
    } catch (err: unknown) {
      let errorMessage = '인증 코드 발송에 실패했습니다.';
      if (axios.isAxiosError(err)) {
        if (err.response?.data?.message) {
          errorMessage = err.response.data.message;
        } else {
          errorMessage = '서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.';
        }
      }
      setError(errorMessage);
    } finally {
      setIsLoading(false);
    }
  };

  // Step 2: 인증 코드 확인
  const handleCodeChange = (index: number, value: string) => {
    if (value && !/^\d$/.test(value)) return;

    const newCode = [...code];
    newCode[index] = value;
    setCode(newCode);
    setError('');

    if (value && index < 5) {
      inputRefs.current[index + 1]?.focus();
    }
  };

  const handleKeyDown = (index: number, e: React.KeyboardEvent) => {
    if (e.key === 'Backspace' && !code[index] && index > 0) {
      inputRefs.current[index - 1]?.focus();
    }
  };

  const handlePaste = (e: React.ClipboardEvent) => {
    e.preventDefault();
    const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, 6);
    if (pasted.length === 6) {
      setCode(pasted.split(''));
      inputRefs.current[5]?.focus();
    }
  };

  const handleVerifySubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const fullCode = code.join('');
    if (fullCode.length !== 6) {
      setError('6자리 인증 코드를 입력해주세요.');
      return;
    }

    setIsLoading(true);
    setError('');

    try {
      const response = await authService.verifyEmail({ email, code: fullCode });
      setSignupToken(response.signupToken);
      setStep('signup');
    } catch (err: unknown) {
      let errorMessage = '인증에 실패했습니다.';
      if (axios.isAxiosError(err) && err.response?.data?.message) {
        errorMessage = err.response.data.message;
      }
      setError(errorMessage);
      setCode(['', '', '', '', '', '']);
      inputRefs.current[0]?.focus();
    } finally {
      setIsLoading(false);
    }
  };

  const handleResend = async () => {
    if (countdown > 0) return;

    setIsLoading(true);
    setError('');

    try {
      await authService.resendVerificationCode(email);
      setCountdown(60);
      setCode(['', '', '', '', '', '']);
      setSignupToken('');
      inputRefs.current[0]?.focus();
    } catch (err: unknown) {
      let errorMessage = '재발송에 실패했습니다.';
      if (axios.isAxiosError(err)) {
        if (err.response?.data?.message) {
          errorMessage = err.response.data.message;
        } else {
          errorMessage = '서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.';
        }
      }
      setError(errorMessage);
    } finally {
      setIsLoading(false);
    }
  };

  // Step 3: 회원가입 완료
  const handleSignupSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError('');

    if (formData.password !== formData.passwordConfirm) {
      setError('비밀번호가 일치하지 않습니다.');
      setIsLoading(false);
      return;
    }

    if (formData.password.length < 8) {
      setError('비밀번호는 8자 이상이어야 합니다.');
      setIsLoading(false);
      return;
    }

    try {
      const response = await authService.signup({
        email,
        password: formData.password,
        name: formData.name,
        signupToken,
      });
      setAuth(response.user, response.accessToken);
      navigate('/');
    } catch (err: unknown) {
      let errorMessage = '회원가입에 실패했습니다.';
      if (axios.isAxiosError(err) && err.response?.data?.message) {
        errorMessage = err.response.data.message;
      }
      setError(errorMessage);
    } finally {
      setIsLoading(false);
    }
  };

  const handleFormChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
    setError('');
  };

  // Step 1: 이메일 입력
  if (step === 'email') {
    return (
      <div className="min-h-[calc(100vh-64px)] flex items-center justify-center py-12 px-4">
        <div className="w-full max-w-md">
          <div className="card">
            <div className="text-center mb-8">
              <h1 className="text-2xl font-bold text-neutral-900 mb-2">회원가입</h1>
              <p className="text-neutral-600">이메일 인증 후 가입이 완료됩니다</p>
            </div>

            <form onSubmit={handleEmailSubmit} className="space-y-5">
              {error && (
                <div className="bg-red-50 text-red-600 px-4 py-3 rounded-xl text-sm">
                  {error}
                </div>
              )}

              <div>
                <label htmlFor="email" className="label">이메일</label>
                <div className="relative">
                  <Mail className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-neutral-400" />
                  <input
                    type="email"
                    id="email"
                    value={email}
                    onChange={(e) => { setEmail(e.target.value); setError(''); }}
                    className="input-field pl-12"
                    placeholder="example@email.com"
                    required
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={isLoading || !email}
                className="btn-primary w-full disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isLoading ? '발송 중...' : '인증 코드 받기'}
              </button>
            </form>

            <div className="mt-6 text-center">
              <p className="text-neutral-600 text-sm">
                이미 계정이 있으신가요?{' '}
                <Link to="/login" className="text-primary-600 font-medium hover:underline">
                  로그인
                </Link>
              </p>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // Step 2: 인증 코드 입력
  if (step === 'verify') {
    return (
      <div className="min-h-[calc(100vh-64px)] flex items-center justify-center py-12 px-4">
        <div className="w-full max-w-md">
          <button
            onClick={() => { setStep('email'); setSignupToken(''); }}
            className="inline-flex items-center gap-2 text-neutral-600 hover:text-neutral-900 mb-6"
          >
            <ArrowLeft className="w-4 h-4" />
            이메일 변경
          </button>

          <div className="card">
            <div className="w-16 h-16 bg-primary-100 rounded-full flex items-center justify-center mx-auto mb-6">
              <Mail className="w-8 h-8 text-primary-600" />
            </div>

            <h1 className="text-2xl font-bold text-neutral-900 text-center mb-2">
              이메일 인증
            </h1>
            <p className="text-neutral-600 text-center mb-8">
              <span className="font-medium text-neutral-900">{email}</span>
              <br />
              으로 발송된 6자리 인증 코드를 입력해주세요.
            </p>

            <form onSubmit={handleVerifySubmit}>
              {error && (
                <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-6">
                  {error}
                </div>
              )}

              <div className="flex justify-center gap-2 mb-6">
                {code.map((digit, index) => (
                  <input
                    key={index}
                    ref={(el) => { inputRefs.current[index] = el; }}
                    type="text"
                    inputMode="numeric"
                    maxLength={1}
                    value={digit}
                    onChange={(e) => handleCodeChange(index, e.target.value)}
                    onKeyDown={(e) => handleKeyDown(index, e)}
                    onPaste={handlePaste}
                    className="w-12 h-14 text-center text-2xl font-bold border-2 border-neutral-300 rounded-lg focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20 outline-none transition-colors"
                  />
                ))}
              </div>

              <button
                type="submit"
                disabled={isLoading || code.some((d) => !d)}
                className="btn-primary w-full mb-4"
              >
                {isLoading ? '인증 중...' : '인증하기'}
              </button>

              <div className="text-center">
                <p className="text-sm text-neutral-600 mb-2">
                  인증 코드를 받지 못하셨나요?
                </p>
                <button
                  type="button"
                  onClick={handleResend}
                  disabled={isLoading || countdown > 0}
                  className="inline-flex items-center gap-2 text-primary-600 hover:text-primary-700 font-medium disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin' : ''}`} />
                  {countdown > 0 ? `${countdown}초 후 재발송 가능` : '인증 코드 재발송'}
                </button>
              </div>
            </form>
          </div>

          <p className="text-center text-sm text-neutral-500 mt-6">
            인증 코드는 10분간 유효합니다.
          </p>
        </div>
      </div>
    );
  }

  // Step 3: 회원가입 완료
  return (
    <div className="min-h-[calc(100vh-64px)] flex items-center justify-center py-12 px-4">
      <div className="w-full max-w-md">
        <div className="card">
          <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-6">
            <CheckCircle className="w-8 h-8 text-green-600" />
          </div>

          <div className="text-center mb-8">
            <h1 className="text-2xl font-bold text-neutral-900 mb-2">이메일 인증 완료</h1>
            <p className="text-neutral-600">
              <span className="font-medium text-neutral-900">{email}</span>
              <br />
              회원 정보를 입력해주세요.
            </p>
          </div>

          <form onSubmit={handleSignupSubmit} className="space-y-5">
            {error && (
              <div className="bg-red-50 text-red-600 px-4 py-3 rounded-xl text-sm">
                {error}
              </div>
            )}

            <div>
              <label htmlFor="name" className="label">이름</label>
              <div className="relative">
                <User className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-neutral-400" />
                <input
                  type="text"
                  id="name"
                  name="name"
                  value={formData.name}
                  onChange={handleFormChange}
                  className="input-field pl-12"
                  placeholder="이름을 입력하세요"
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
                  onChange={handleFormChange}
                  className="input-field pl-12 pr-12"
                  placeholder="8자 이상 입력하세요"
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

            <div>
              <label htmlFor="passwordConfirm" className="label">비밀번호 확인</label>
              <div className="relative">
                <Lock className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-neutral-400" />
                <input
                  type={showPassword ? 'text' : 'password'}
                  id="passwordConfirm"
                  name="passwordConfirm"
                  value={formData.passwordConfirm}
                  onChange={handleFormChange}
                  className="input-field pl-12"
                  placeholder="비밀번호를 다시 입력하세요"
                  required
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={isLoading || !signupToken}
              className="btn-primary w-full disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isLoading ? '가입 중...' : '회원가입 완료'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
