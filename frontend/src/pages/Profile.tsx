import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { User, Briefcase, Code, Globe, Clock, Users, Target, Lightbulb } from 'lucide-react';
import { profileService, type ProfileRequest } from '../services/profileService';
import { useAuthStore } from '../stores/authStore';
import type { Profile } from '../types';

const JOB_ROLES = [
  { value: 'PLANNER', label: '기획자' },
  { value: 'UXUI', label: 'UX/UI 디자이너' },
  { value: 'FRONTEND', label: '프론트엔드 개발자' },
  { value: 'BACKEND', label: '백엔드 개발자' },
];

const COLLABORATION_STYLES = [
  { value: 'LEADER', label: '리더형', desc: '주도적으로 이끌기 선호' },
  { value: 'FOLLOWER', label: '팔로워형', desc: '지시에 따라 수행 선호' },
  { value: 'COLLABORATIVE', label: '협업형', desc: '함께 논의하며 진행 선호' },
  { value: 'INDEPENDENT', label: '독립형', desc: '맡은 부분 독립적으로 진행' },
];

const DIFFICULTIES = [
  { value: 'BEGINNER', label: '입문', desc: '기초적인 프로젝트' },
  { value: 'INTERMEDIATE', label: '중급', desc: '일반적인 수준의 프로젝트' },
  { value: 'ADVANCED', label: '고급', desc: '도전적인 프로젝트' },
];

const TECH_STACK_OPTIONS = [
  'React', 'Vue', 'Angular', 'Next.js', 'TypeScript',
  'Java', 'Spring Boot', 'Node.js', 'Python', 'Django',
  'PostgreSQL', 'MySQL', 'MongoDB', 'Redis',
  'AWS', 'Docker', 'Kubernetes', 'Figma', 'Adobe XD',
];

const DOMAIN_OPTIONS = [
  '이커머스', '핀테크', '헬스케어', '교육', 'AI/ML',
  '소셜미디어', '생산성', '게임', '콘텐츠', 'B2B SaaS',
];

export default function ProfilePage() {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuthStore();
  const [profile, setProfile] = useState<Profile | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const [formData, setFormData] = useState<ProfileRequest>({
    jobRole: '',
    techStacks: [],
    portfolioUrl: '',
    interestedDomains: [],
    availableHoursPerWeek: undefined,
    collaborationStyle: '',
    improvementGoal: '',
    preferredDifficulty: '',
    introduction: '',
  });

  // 비로그인 사용자 리다이렉트
  useEffect(() => {
    if (!isAuthenticated) {
      navigate('/login', { replace: true });
    }
  }, [isAuthenticated, navigate]);

  useEffect(() => {
    if (isAuthenticated) {
      loadProfile();
    }
  }, [isAuthenticated]);

  const loadProfile = async () => {
    try {
      const data = await profileService.getMyProfile();
      if (data) {
        setProfile(data);
        setFormData({
          jobRole: data.jobRole,
          techStacks: data.techStacks || [],
          portfolioUrl: data.portfolioUrl || '',
          interestedDomains: data.interestedDomains || [],
          availableHoursPerWeek: data.availableHoursPerWeek,
          collaborationStyle: data.collaborationStyle || '',
          improvementGoal: data.improvementGoal || '',
          preferredDifficulty: data.preferredDifficulty || '',
          introduction: data.introduction || '',
        });
      }
    } catch (err) {
      console.error('Failed to load profile:', err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    setError('');
    setSuccess('');
  };

  const handleNumberChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value ? parseInt(value) : undefined }));
  };

  const toggleArrayItem = (field: 'techStacks' | 'interestedDomains', item: string) => {
    setFormData(prev => {
      const current = prev[field];
      const updated = current.includes(item)
        ? current.filter(i => i !== item)
        : [...current, item];
      return { ...prev, [field]: updated };
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSaving(true);
    setError('');
    setSuccess('');

    if (!formData.jobRole) {
      setError('역할을 선택해주세요.');
      setIsSaving(false);
      return;
    }

    try {
      const savedProfile = await profileService.saveProfile(formData);
      setProfile(savedProfile);
      setSuccess('프로필이 저장되었습니다.');
    } catch (err: unknown) {
      const errorMessage = err instanceof Error && 'response' in err
        ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
        : null;
      setError(errorMessage || '프로필 저장에 실패했습니다.');
    } finally {
      setIsSaving(false);
    }
  };

  // 비로그인 상태면 렌더링하지 않음
  if (!isAuthenticated) {
    return null;
  }

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[50vh]">
        <div className="text-neutral-500">로딩 중...</div>
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto px-4 py-12">
      <div className="text-center mb-8">
        <User className="w-16 h-16 text-primary-500 mx-auto mb-4" />
        <h1 className="text-2xl font-bold text-neutral-900 mb-2">
          {profile ? '프로필 수정' : '프로필 설정'}
        </h1>
        <p className="text-neutral-600">
          팀 매칭과 AI 피드백을 위해 프로필을 작성해주세요.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        {error && (
          <div className="bg-red-50 text-red-600 px-4 py-3 rounded-xl text-sm">
            {error}
          </div>
        )}
        {success && (
          <div className="bg-green-50 text-green-600 px-4 py-3 rounded-xl text-sm">
            {success}
          </div>
        )}

        {/* 역할 선택 */}
        <div className="card">
          <div className="flex items-center gap-2 mb-4">
            <Briefcase className="w-5 h-5 text-primary-500" />
            <h2 className="font-semibold text-neutral-900">역할</h2>
            <span className="text-red-500">*</span>
          </div>
          <div className="grid grid-cols-2 gap-3">
            {JOB_ROLES.map(role => (
              <label
                key={role.value}
                className={`flex items-center justify-center p-4 rounded-xl border-2 cursor-pointer transition-all ${
                  formData.jobRole === role.value
                    ? 'border-primary-500 bg-primary-50 text-primary-700'
                    : 'border-neutral-200 hover:border-primary-200'
                }`}
              >
                <input
                  type="radio"
                  name="jobRole"
                  value={role.value}
                  checked={formData.jobRole === role.value}
                  onChange={handleChange}
                  className="sr-only"
                />
                <span className="font-medium">{role.label}</span>
              </label>
            ))}
          </div>
        </div>

        {/* 기술 스택 */}
        <div className="card">
          <div className="flex items-center gap-2 mb-4">
            <Code className="w-5 h-5 text-primary-500" />
            <h2 className="font-semibold text-neutral-900">기술 스택</h2>
          </div>
          <div className="flex flex-wrap gap-2">
            {TECH_STACK_OPTIONS.map(tech => (
              <button
                key={tech}
                type="button"
                onClick={() => toggleArrayItem('techStacks', tech)}
                className={`px-3 py-1.5 rounded-full text-sm transition-all ${
                  formData.techStacks.includes(tech)
                    ? 'bg-primary-500 text-white'
                    : 'bg-neutral-100 text-neutral-700 hover:bg-neutral-200'
                }`}
              >
                {tech}
              </button>
            ))}
          </div>
        </div>

        {/* 관심 도메인 */}
        <div className="card">
          <div className="flex items-center gap-2 mb-4">
            <Globe className="w-5 h-5 text-primary-500" />
            <h2 className="font-semibold text-neutral-900">관심 도메인</h2>
          </div>
          <div className="flex flex-wrap gap-2">
            {DOMAIN_OPTIONS.map(domain => (
              <button
                key={domain}
                type="button"
                onClick={() => toggleArrayItem('interestedDomains', domain)}
                className={`px-3 py-1.5 rounded-full text-sm transition-all ${
                  formData.interestedDomains.includes(domain)
                    ? 'bg-secondary-500 text-white'
                    : 'bg-neutral-100 text-neutral-700 hover:bg-neutral-200'
                }`}
              >
                {domain}
              </button>
            ))}
          </div>
        </div>

        {/* 가용 시간 및 협업 성향 */}
        <div className="grid md:grid-cols-2 gap-6">
          <div className="card">
            <div className="flex items-center gap-2 mb-4">
              <Clock className="w-5 h-5 text-primary-500" />
              <h2 className="font-semibold text-neutral-900">주당 가용 시간</h2>
            </div>
            <input
              type="number"
              name="availableHoursPerWeek"
              value={formData.availableHoursPerWeek || ''}
              onChange={handleNumberChange}
              className="input-field"
              placeholder="예: 20"
              min="1"
              max="100"
            />
            <p className="mt-1.5 text-xs text-neutral-500">시간 단위로 입력</p>
          </div>

          <div className="card">
            <div className="flex items-center gap-2 mb-4">
              <Users className="w-5 h-5 text-primary-500" />
              <h2 className="font-semibold text-neutral-900">협업 성향</h2>
            </div>
            <select
              name="collaborationStyle"
              value={formData.collaborationStyle}
              onChange={handleChange}
              className="input-field"
            >
              <option value="">선택하세요</option>
              {COLLABORATION_STYLES.map(style => (
                <option key={style.value} value={style.value}>
                  {style.label} - {style.desc}
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* 희망 난이도 */}
        <div className="card">
          <div className="flex items-center gap-2 mb-4">
            <Target className="w-5 h-5 text-primary-500" />
            <h2 className="font-semibold text-neutral-900">희망 프로젝트 난이도</h2>
          </div>
          <div className="grid grid-cols-3 gap-3">
            {DIFFICULTIES.map(diff => (
              <label
                key={diff.value}
                className={`flex flex-col items-center p-4 rounded-xl border-2 cursor-pointer transition-all ${
                  formData.preferredDifficulty === diff.value
                    ? 'border-primary-500 bg-primary-50'
                    : 'border-neutral-200 hover:border-primary-200'
                }`}
              >
                <input
                  type="radio"
                  name="preferredDifficulty"
                  value={diff.value}
                  checked={formData.preferredDifficulty === diff.value}
                  onChange={handleChange}
                  className="sr-only"
                />
                <span className="font-medium text-neutral-900">{diff.label}</span>
                <span className="text-xs text-neutral-500 mt-1">{diff.desc}</span>
              </label>
            ))}
          </div>
        </div>

        {/* 포트폴리오 URL */}
        <div className="card">
          <div className="flex items-center gap-2 mb-4">
            <Globe className="w-5 h-5 text-primary-500" />
            <h2 className="font-semibold text-neutral-900">포트폴리오 URL</h2>
          </div>
          <input
            type="url"
            name="portfolioUrl"
            value={formData.portfolioUrl}
            onChange={handleChange}
            className="input-field"
            placeholder="https://..."
          />
        </div>

        {/* 보완 역량 및 소개 */}
        <div className="card">
          <div className="flex items-center gap-2 mb-4">
            <Lightbulb className="w-5 h-5 text-primary-500" />
            <h2 className="font-semibold text-neutral-900">보완하고 싶은 역량</h2>
          </div>
          <input
            type="text"
            name="improvementGoal"
            value={formData.improvementGoal}
            onChange={handleChange}
            className="input-field"
            placeholder="예: 협업 능력, 코드 리뷰 경험"
          />
        </div>

        <div className="card">
          <div className="flex items-center gap-2 mb-4">
            <User className="w-5 h-5 text-primary-500" />
            <h2 className="font-semibold text-neutral-900">자기소개</h2>
          </div>
          <textarea
            name="introduction"
            value={formData.introduction}
            onChange={handleChange}
            className="input-field min-h-[120px] resize-none"
            placeholder="팀원들에게 보여줄 간단한 자기소개를 작성해주세요."
          />
        </div>

        <div className="flex gap-4">
          <button
            type="button"
            onClick={() => navigate(-1)}
            className="btn-secondary flex-1"
          >
            취소
          </button>
          <button
            type="submit"
            disabled={isSaving}
            className="btn-primary flex-1 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isSaving ? '저장 중...' : '프로필 저장'}
          </button>
        </div>
      </form>
    </div>
  );
}
