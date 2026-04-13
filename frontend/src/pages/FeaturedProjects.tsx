import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowLeft, Award, MonitorPlay, Users } from 'lucide-react';
import { projectService } from '../services/projectService';
import type { Project } from '../types';

export default function FeaturedProjectsPage() {
  const [projects, setProjects] = useState<Project[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const loadProjects = async () => {
      try {
        const data = await projectService.getFeaturedProjects();
        setProjects(data);
      } catch (err) {
        console.error('Failed to load featured projects:', err);
        setError('우수작 정보를 불러오는데 실패했습니다.');
      } finally {
        setIsLoading(false);
      }
    };

    void loadProjects();
  }, []);

  const projectsBySeason = useMemo(() => {
    return projects.reduce<Record<string, Project[]>>((acc, project) => {
      const seasonTitle = project.seasonTitle ?? '기타 시즌';
      acc[seasonTitle] = [...(acc[seasonTitle] ?? []), project];
      return acc;
    }, {});
  }, [projects]);

  if (isLoading) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center">
        <div className="text-neutral-500">우수작을 불러오는 중...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="mx-auto max-w-4xl px-4 py-12">
        <div className="rounded-2xl border border-red-200 bg-red-50 p-6 text-center text-red-700">{error}</div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-12">
      <Link to="/" className="mb-6 inline-flex items-center gap-2 text-neutral-600 hover:text-neutral-900">
        <ArrowLeft className="h-5 w-5" />
        메인으로
      </Link>

      <div className="mb-10 rounded-3xl bg-gradient-to-br from-amber-50 via-white to-orange-50 p-8">
        <div className="mb-4 inline-flex items-center gap-2 rounded-full bg-amber-100 px-4 py-2 text-sm font-medium text-amber-700">
          <Award className="h-4 w-4" />
          시즌 우수작
        </div>
        <h1 className="mb-3 text-3xl font-bold text-neutral-900">심사를 통해 선정된 프로젝트</h1>
        <p className="max-w-2xl text-neutral-600">
          운영자가 시즌 심사 결과를 검토한 뒤 확정한 우수작입니다. 문제 정의, 사용자 가치, 구현 완성도가 돋보인 프로젝트를 시즌별로 확인할 수 있습니다.
        </p>
      </div>

      {projects.length === 0 ? (
        <div className="rounded-2xl border border-neutral-200 bg-white p-10 text-center text-neutral-500">
          아직 공개된 우수작이 없습니다.
        </div>
      ) : (
        <div className="space-y-10">
          {Object.entries(projectsBySeason).map(([seasonTitle, seasonProjects]) => (
            <section key={seasonTitle}>
              <div className="mb-4 flex items-center gap-3">
                <h2 className="text-2xl font-bold text-neutral-900">{seasonTitle}</h2>
                <span className="rounded-full bg-neutral-100 px-3 py-1 text-sm text-neutral-600">
                  {seasonProjects.length}개 우수작
                </span>
              </div>

              <div className="grid gap-6 md:grid-cols-2 xl:grid-cols-3">
                {seasonProjects.map((project) => (
                  <article key={project.id} className="rounded-2xl border border-neutral-200 bg-white p-6 shadow-sm">
                    <div className="mb-4 flex items-center justify-between gap-3">
                      <span className="rounded-full bg-amber-100 px-3 py-1 text-sm font-semibold text-amber-700">
                        우수작 #{project.featuredRank}
                      </span>
                      <span className="text-sm text-neutral-500">{project.teamName}</span>
                    </div>

                    <h3 className="mb-2 text-xl font-semibold text-neutral-900">{project.name}</h3>
                    <p className="mb-4 line-clamp-4 min-h-[6rem] text-sm leading-6 text-neutral-600">
                      {project.problemDefinition || '프로젝트 소개가 아직 등록되지 않았습니다.'}
                    </p>

                    <div className="mb-5 flex items-center gap-2 text-sm text-neutral-500">
                      <Users className="h-4 w-4" />
                      {project.teamName}
                    </div>

                    <div className="flex flex-wrap gap-2">
                      {project.demoUrl && (
                        <a
                          href={project.demoUrl}
                          target="_blank"
                          rel="noreferrer"
                          className="inline-flex items-center gap-2 rounded-lg bg-primary-500 px-3 py-2 text-sm font-medium text-white hover:bg-primary-600"
                        >
                          <MonitorPlay className="h-4 w-4" />
                          데모 보기
                        </a>
                      )}
                      {project.githubUrl && (
                        <a
                          href={project.githubUrl}
                          target="_blank"
                          rel="noreferrer"
                          className="inline-flex items-center gap-2 rounded-lg border border-neutral-300 px-3 py-2 text-sm font-medium text-neutral-700 hover:bg-neutral-50"
                        >
                          GitHub
                        </a>
                      )}
                    </div>
                  </article>
                ))}
              </div>
            </section>
          ))}
        </div>
      )}
    </div>
  );
}
