import { useState, useEffect, useRef } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Mail, RefreshCw, CheckCircle, ArrowLeft } from 'lucide-react';
import axios from 'axios';
import { authService } from '../services/authService';
import { useAuthStore } from '../stores/authStore';

export default function EmailVerification() {
  const navigate = useNavigate();
  const { user, refreshUser } = useAuthStore();
  const [code, setCode] = useState(['', '', '', '', '', '']);
  const [isLoading, setIsLoading] = useState(false);
  const [isResending, setIsResending] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [countdown, setCountdown] = useState(0);
  const inputRefs = useRef<(HTMLInputElement | null)[]>([]);

  useEffect(() => {
    // 이미 인증된 경우 홈으로
    if (user?.emailVerified) {
      navigate('/');
    }
  }, [user, navigate]);

  useEffect(() => {
    // 재발송 카운트다운
    if (countdown > 0) {
      const timer = setTimeout(() => setCountdown(countdown - 1), 1000);
      return () => clearTimeout(timer);
    }
  }, [countdown]);

  const handleChange = (index: number, value: string) => {
    // 숫자만 허용
    if (value && !/^\d$/.test(value)) return;

    const newCode = [...code];
    newCode[index] = value;
    setCode(newCode);
    setError('');

    // 다음 입력으로 자동 이동
    if (value && index < 5) {
      inputRefs.current[index + 1]?.focus();
    }
  };

  const handleKeyDown = (index: number, e: React.KeyboardEvent) => {
    // 백스페이스로 이전 입력으로 이동
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

  const handleVerify = async () => {
    const fullCode = code.join('');
    if (fullCode.length !== 6) {
      setError('6자리 인증 코드를 입력해주세요.');
      return;
    }

    if (!user?.email) {
      setError('로그인 정보를 찾을 수 없습니다.');
      return;
    }

    setIsLoading(true);
    setError('');

    try {
      await authService.verifyEmail({ email: user.email, code: fullCode });
      setSuccess(true);
      await refreshUser();
      setTimeout(() => navigate('/'), 2000);
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
    if (!user?.email || countdown > 0) return;

    setIsResending(true);
    setError('');

    try {
      await authService.resendVerificationCode(user.email);
      setCountdown(60);
      setCode(['', '', '', '', '', '']);
      inputRefs.current[0]?.focus();
    } catch (err: unknown) {
      let errorMessage = '재발송에 실패했습니다.';
      if (axios.isAxiosError(err) && err.response?.data?.message) {
        errorMessage = err.response.data.message;
      }
      setError(errorMessage);
    } finally {
      setIsResending(false);
    }
  };

  if (success) {
    return (
      <div className="min-h-[70vh] flex items-center justify-center px-4">
        <div className="max-w-md w-full text-center">
          <div className="w-20 h-20 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-6">
            <CheckCircle className="w-10 h-10 text-green-600" />
          </div>
          <h1 className="text-2xl font-bold text-neutral-900 mb-2">
            이메일 인증 완료
          </h1>
          <p className="text-neutral-600 mb-6">
            이메일 인증이 완료되었습니다.<br />
            잠시 후 홈으로 이동합니다.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-[70vh] flex items-center justify-center px-4">
      <div className="max-w-md w-full">
        <Link
          to="/"
          className="inline-flex items-center gap-2 text-neutral-600 hover:text-neutral-900 mb-6"
        >
          <ArrowLeft className="w-4 h-4" />
          홈으로
        </Link>

        <div className="card">
          <div className="w-16 h-16 bg-primary-100 rounded-full flex items-center justify-center mx-auto mb-6">
            <Mail className="w-8 h-8 text-primary-600" />
          </div>

          <h1 className="text-2xl font-bold text-neutral-900 text-center mb-2">
            이메일 인증
          </h1>
          <p className="text-neutral-600 text-center mb-8">
            <span className="font-medium text-neutral-900">{user?.email}</span>
            <br />
            으로 발송된 6자리 인증 코드를 입력해주세요.
          </p>

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
                onChange={(e) => handleChange(index, e.target.value)}
                onKeyDown={(e) => handleKeyDown(index, e)}
                onPaste={handlePaste}
                className="w-12 h-14 text-center text-2xl font-bold border-2 border-neutral-300 rounded-lg focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20 outline-none transition-colors"
              />
            ))}
          </div>

          <button
            onClick={handleVerify}
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
              onClick={handleResend}
              disabled={isResending || countdown > 0}
              className="inline-flex items-center gap-2 text-primary-600 hover:text-primary-700 font-medium disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <RefreshCw className={`w-4 h-4 ${isResending ? 'animate-spin' : ''}`} />
              {countdown > 0 ? `${countdown}초 후 재발송 가능` : '인증 코드 재발송'}
            </button>
          </div>
        </div>

        <p className="text-center text-sm text-neutral-500 mt-6">
          인증 코드는 10분간 유효합니다.
        </p>
      </div>
    </div>
  );
}
