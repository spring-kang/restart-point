import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { Award, Building2, Calendar, Upload, CheckCircle, Clock, XCircle, RefreshCw, X, FileImage } from 'lucide-react';
import { useAuthStore } from '../stores/authStore';
import { authService } from '../services/authService';
import { fileService } from '../services/fileService';

export default function Certification() {
  const navigate = useNavigate();
  const { user, updateUser, isAuthenticated } = useAuthStore();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [formData, setFormData] = useState({
    bootcampName: '',
    bootcampGeneration: '',
    graduationDate: '',
    certificateUrl: '',
  });
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
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

  // 파일 선택 핸들러
  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // 파일 크기 검사 (10MB)
    if (file.size > 10 * 1024 * 1024) {
      setError('파일 크기는 10MB를 초과할 수 없습니다.');
      return;
    }

    // 허용된 파일 형식 검사
    const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'application/pdf'];
    if (!allowedTypes.includes(file.type)) {
      setError('허용되지 않는 파일 형식입니다. (허용: jpg, png, gif, webp, pdf)');
      return;
    }

    setSelectedFile(file);
    setError('');

    // 이미지 미리보기 생성
    if (file.type.startsWith('image/')) {
      const reader = new FileReader();
      reader.onload = (e) => {
        setPreviewUrl(e.target?.result as string);
      };
      reader.readAsDataURL(file);
    } else {
      setPreviewUrl(null);
    }
  };

  // 파일 선택 취소
  const handleFileRemove = () => {
    setSelectedFile(null);
    setPreviewUrl(null);
    setFormData({ ...formData, certificateUrl: '' });
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  // 드래그 앤 드롭 핸들러
  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    const file = e.dataTransfer.files?.[0];
    if (file) {
      const input = fileInputRef.current;
      if (input) {
        const dataTransfer = new DataTransfer();
        dataTransfer.items.add(file);
        input.files = dataTransfer.files;
        handleFileSelect({ target: { files: dataTransfer.files } } as React.ChangeEvent<HTMLInputElement>);
      }
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError('');

    try {
      let certificateUrl = formData.certificateUrl;

      // 파일이 선택되었으면 먼저 업로드
      if (selectedFile && !certificateUrl) {
        setIsUploading(true);
        try {
          certificateUrl = await fileService.uploadFile(selectedFile, 'certificates');
        } catch {
          setError('파일 업로드에 실패했습니다. 다시 시도해주세요.');
          setIsLoading(false);
          setIsUploading(false);
          return;
        }
        setIsUploading(false);
      }

      if (!certificateUrl) {
        setError('수료증 파일을 업로드해주세요.');
        setIsLoading(false);
        return;
      }

      // 인증 요청
      const updatedUser = await authService.requestCertification({
        ...formData,
        certificateUrl,
      });
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
          부트캠프 수료생이신가요? 인증하면 수료생 전용 시즌에 참여할 수 있습니다.
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
            <label className="label">수료증 업로드</label>
            <input
              ref={fileInputRef}
              type="file"
              accept="image/jpeg,image/png,image/gif,image/webp,application/pdf"
              onChange={handleFileSelect}
              className="hidden"
            />

            {!selectedFile ? (
              <div
                onClick={() => fileInputRef.current?.click()}
                onDragOver={handleDragOver}
                onDrop={handleDrop}
                className="border-2 border-dashed border-neutral-300 rounded-xl p-8 text-center cursor-pointer hover:border-primary-400 hover:bg-primary-50/50 transition-colors"
              >
                <Upload className="w-10 h-10 text-neutral-400 mx-auto mb-3" />
                <p className="text-neutral-600 mb-1">
                  클릭하거나 파일을 드래그하여 업로드
                </p>
                <p className="text-xs text-neutral-400">
                  JPG, PNG, GIF, WEBP, PDF (최대 10MB)
                </p>
              </div>
            ) : (
              <div className="border border-neutral-200 rounded-xl p-4">
                <div className="flex items-start gap-4">
                  {previewUrl ? (
                    <img
                      src={previewUrl}
                      alt="수료증 미리보기"
                      className="w-20 h-20 object-cover rounded-lg"
                    />
                  ) : (
                    <div className="w-20 h-20 bg-neutral-100 rounded-lg flex items-center justify-center">
                      <FileImage className="w-8 h-8 text-neutral-400" />
                    </div>
                  )}
                  <div className="flex-1 min-w-0">
                    <p className="font-medium text-neutral-900 truncate">
                      {selectedFile.name}
                    </p>
                    <p className="text-sm text-neutral-500">
                      {(selectedFile.size / 1024 / 1024).toFixed(2)} MB
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={handleFileRemove}
                    className="p-2 text-neutral-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-colors"
                  >
                    <X className="w-5 h-5" />
                  </button>
                </div>
              </div>
            )}
          </div>

          <button
            type="submit"
            disabled={isLoading || !selectedFile}
            className="btn-primary w-full disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isUploading ? '파일 업로드 중...' : isLoading ? '요청 중...' : '수료 인증 요청'}
          </button>
        </form>
      </div>
    </div>
  );
}
