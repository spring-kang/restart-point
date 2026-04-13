import { Link } from 'react-router-dom';
import { ArrowRight, Users, Target, TrendingUp, Sparkles } from 'lucide-react';
import { useAuthStore } from '../stores/authStore';

export default function Home() {
  const { isAuthenticated } = useAuthStore();

  const features = [
    {
      icon: <Users className="w-6 h-6" />,
      title: 'AI 팀 매칭',
      description: '역할, 기술 스택, 협업 성향을 분석해 최적의 팀원을 추천받으세요.',
    },
    {
      icon: <Target className="w-6 h-6" />,
      title: '프로젝트 코칭',
      description: 'AI가 프로젝트 진행 상황을 분석하고 구체적인 개선 방향을 제시합니다.',
    },
    {
      icon: <TrendingUp className="w-6 h-6" />,
      title: '성장 리포트',
      description: '심사 결과를 바탕으로 개인별 성장 포인트와 다음 액션을 받아보세요.',
    },
    {
      icon: <Sparkles className="w-6 h-6" />,
      title: '실전 협업',
      description: '기획, UX/UI, 프론트엔드, 백엔드가 함께하는 실전 프로젝트를 경험하세요.',
    },
  ];

  return (
    <div>
      {/* 히어로 섹션 */}
      <section className="bg-gradient-to-br from-primary-50 via-white to-secondary-50 py-20 lg:py-32">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-3xl mx-auto">
            <div className="inline-flex items-center gap-2 bg-primary-100 text-primary-700 px-4 py-2 rounded-full text-sm font-medium mb-6">
              <Sparkles className="w-4 h-4" />
              AI 기반 프로젝트 러닝 플랫폼
            </div>
            <h1 className="text-4xl lg:text-6xl font-bold text-neutral-900 mb-6 leading-tight">
              부트캠프 수료 이후,
              <br />
              <span className="text-primary-600">성장을 다시 시작하세요</span>
            </h1>
            <p className="text-lg text-neutral-600 mb-10 leading-relaxed">
              혼자가 아닌 팀으로, 점수가 아닌 성장 중심으로.
              <br />
              Re:Start Point에서 실전 협업과 AI 피드백으로 다음 단계를 준비하세요.
            </p>
            <div className="flex flex-col sm:flex-row gap-4 justify-center">
              {isAuthenticated ? (
                <>
                  <Link to="/seasons" className="btn-primary text-base px-8 py-3 flex items-center justify-center gap-2">
                    시즌 참여하기
                    <ArrowRight className="w-5 h-5" />
                  </Link>
                  <Link to="/featured-projects" className="btn-secondary text-base px-8 py-3">
                    우수작 보기
                  </Link>
                </>
              ) : (
                <>
                  <Link to="/signup" className="btn-primary text-base px-8 py-3 flex items-center justify-center gap-2">
                    시작하기
                    <ArrowRight className="w-5 h-5" />
                  </Link>
                  <Link to="/seasons" className="btn-secondary text-base px-8 py-3">
                    시즌 둘러보기
                  </Link>
                  <Link to="/featured-projects" className="btn-secondary text-base px-8 py-3">
                    우수작 보기
                  </Link>
                </>
              )}
            </div>
          </div>
        </div>
      </section>

      {/* 문제 제기 섹션 */}
      <section className="py-20 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="max-w-3xl mx-auto text-center mb-16">
            <h2 className="text-3xl font-bold text-neutral-900 mb-4">
              수료 이후, 이런 고민 있으셨나요?
            </h2>
            <p className="text-neutral-600">
              부트캠프는 수료까지는 체계적이지만, 수료 이후는 개인에게 맡겨집니다.
            </p>
          </div>

          <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6">
            {[
              { emoji: '😔', text: '학습 루틴이 깨지며 성장이 정체됐어요' },
              { emoji: '👤', text: '포트폴리오를 혼자 만들고 있어요' },
              { emoji: '🤝', text: '적합한 팀원을 구하기 어려워요' },
              { emoji: '❓', text: '프로젝트의 부족한 점을 모르겠어요' },
            ].map((item, index) => (
              <div key={index} className="card text-center">
                <span className="text-4xl mb-4 block">{item.emoji}</span>
                <p className="text-neutral-700">{item.text}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* 기능 소개 섹션 */}
      <section className="py-20 bg-neutral-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16">
            <h2 className="text-3xl font-bold text-neutral-900 mb-4">
              Re:Start Point가 도와드릴게요
            </h2>
            <p className="text-neutral-600">
              AI 기반 팀 매칭부터 성장 리포트까지, 수료 이후의 학습을 지원합니다.
            </p>
          </div>

          <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6">
            {features.map((feature, index) => (
              <div key={index} className="card-hover">
                <div className="w-12 h-12 bg-primary-100 rounded-xl flex items-center justify-center text-primary-600 mb-4">
                  {feature.icon}
                </div>
                <h3 className="font-semibold text-neutral-900 mb-2">{feature.title}</h3>
                <p className="text-sm text-neutral-600">{feature.description}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* 진행 과정 섹션 */}
      <section className="py-20 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16">
            <h2 className="text-3xl font-bold text-neutral-900 mb-4">
              이렇게 진행됩니다
            </h2>
          </div>

          <div className="grid md:grid-cols-5 gap-4">
            {[
              { step: '01', title: '수료 인증', desc: '부트캠프 수료 인증' },
              { step: '02', title: '프로필 작성', desc: '역할 및 기술 스택 입력' },
              { step: '03', title: '팀 구성', desc: 'AI 추천으로 팀 매칭' },
              { step: '04', title: '프로젝트', desc: 'AI 코칭과 함께 진행' },
              { step: '05', title: '성장 리포트', desc: '개인별 피드백 수령' },
            ].map((item, index) => (
              <div key={index} className="text-center">
                <div className="w-12 h-12 bg-primary-500 text-white rounded-full flex items-center justify-center font-bold mx-auto mb-3">
                  {item.step}
                </div>
                <h4 className="font-semibold text-neutral-900 mb-1">{item.title}</h4>
                <p className="text-sm text-neutral-500">{item.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA 섹션 */}
      <section className="py-20 bg-gradient-to-r from-primary-500 to-primary-600">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <h2 className="text-3xl font-bold text-white mb-4">
            지금 바로 시작하세요
          </h2>
          <p className="text-primary-100 mb-8 max-w-2xl mx-auto">
            부트캠프 수료 이후에도 성장을 멈추지 마세요.
            Re:Start Point에서 다시 시작할 수 있습니다.
          </p>
          {!isAuthenticated && (
            <Link
              to="/signup"
              className="inline-flex items-center gap-2 bg-white text-primary-600 font-medium px-8 py-3 rounded-xl hover:bg-primary-50 transition-colors"
            >
              무료로 시작하기
              <ArrowRight className="w-5 h-5" />
            </Link>
          )}
        </div>
      </section>
    </div>
  );
}
