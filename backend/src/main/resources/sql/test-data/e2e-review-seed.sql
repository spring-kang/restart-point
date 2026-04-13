-- Review E2E seed for PostgreSQL
-- Purpose:
-- 1) make a REVIEWING season
-- 2) prepare an expert reviewer account that can login with password test1234
-- 3) prepare another user/team/project that appears in reviewable projects
-- 4) optionally create one existing review history row for UI verification
--
-- Safe usage:
-- - run in dev/staging first
-- - if running in prod, review email/title values before execution
-- - this script is written to be re-runnable as much as possible

BEGIN;

-- 0) users
INSERT INTO users (
  email,
  password,
  name,
  role,
  certification_status,
  email_verified,
  bootcamp_name,
  bootcamp_generation,
  graduation_date,
  certificate_url,
  created_at,
  updated_at
)
VALUES
  (
    'test1@restart-point.com',
    '$2a$10$wNA.hdZDoVEnd3AuFfcV/e.VVrn0J6CXZBuAUKXEFxWYXAcVJjAau',
    'E2E 리뷰어',
    'REVIEWER',
    'APPROVED',
    true,
    'Restart Camp',
    '10기',
    '2026-03-01',
    'https://example.com/certificate/reviewer',
    NOW(),
    NOW()
  ),
  (
    'test2@restart-point.com',
    '$2a$10$wNA.hdZDoVEnd3AuFfcV/e.VVrn0J6CXZBuAUKXEFxWYXAcVJjAau',
    'E2E 팀장',
    'USER',
    'APPROVED',
    true,
    'Restart Camp',
    '10기',
    '2026-03-01',
    'https://example.com/certificate/leader',
    NOW(),
    NOW()
  ),
  (
    'review-target@restart-point.com',
    '$2a$10$wNA.hdZDoVEnd3AuFfcV/e.VVrn0J6CXZBuAUKXEFxWYXAcVJjAau',
    'E2E 일반유저',
    'USER',
    'APPROVED',
    true,
    'Restart Camp',
    '10기',
    '2026-03-01',
    'https://example.com/certificate/member',
    NOW(),
    NOW()
  ),
  (
    'review-admin@restart-point.com',
    '$2a$10$wNA.hdZDoVEnd3AuFfcV/e.VVrn0J6CXZBuAUKXEFxWYXAcVJjAau',
    'E2E 보조 리뷰어',
    'REVIEWER',
    'APPROVED',
    true,
    'Restart Camp',
    '10기',
    '2026-03-01',
    'https://example.com/certificate/review-admin',
    NOW(),
    NOW()
  )
ON CONFLICT (email) DO UPDATE SET
  password = EXCLUDED.password,
  name = EXCLUDED.name,
  role = EXCLUDED.role,
  certification_status = EXCLUDED.certification_status,
  email_verified = EXCLUDED.email_verified,
  bootcamp_name = EXCLUDED.bootcamp_name,
  bootcamp_generation = EXCLUDED.bootcamp_generation,
  graduation_date = EXCLUDED.graduation_date,
  certificate_url = EXCLUDED.certificate_url,
  updated_at = NOW();

-- 1) season: create or force into REVIEWING window
DO $$
DECLARE
  v_season_id BIGINT;
BEGIN
  SELECT id INTO v_season_id
  FROM seasons
  WHERE title = '테스트 시즌'
  ORDER BY id DESC
  LIMIT 1;

  IF v_season_id IS NULL THEN
    INSERT INTO seasons (
      title,
      description,
      status,
      recruitment_start_at,
      recruitment_end_at,
      team_building_start_at,
      team_building_end_at,
      project_start_at,
      project_end_at,
      review_start_at,
      review_end_at,
      expert_review_weight,
      candidate_review_weight,
      created_at,
      updated_at
    ) VALUES (
      '테스트 시즌',
      'E2E 리뷰 테스트용 시즌',
      'REVIEWING',
      NOW() - INTERVAL '30 day',
      NOW() - INTERVAL '20 day',
      NOW() - INTERVAL '19 day',
      NOW() - INTERVAL '15 day',
      NOW() - INTERVAL '14 day',
      NOW() - INTERVAL '1 day',
      NOW() - INTERVAL '1 hour',
      NOW() + INTERVAL '30 day',
      100,
      0,
      NOW(),
      NOW()
    ) RETURNING id INTO v_season_id;
  ELSE
    UPDATE seasons
    SET description = 'E2E 리뷰 테스트용 시즌',
        status = 'REVIEWING',
        recruitment_start_at = NOW() - INTERVAL '30 day',
        recruitment_end_at = NOW() - INTERVAL '20 day',
        team_building_start_at = NOW() - INTERVAL '19 day',
        team_building_end_at = NOW() - INTERVAL '15 day',
        project_start_at = NOW() - INTERVAL '14 day',
        project_end_at = NOW() - INTERVAL '1 day',
        review_start_at = NOW() - INTERVAL '1 hour',
        review_end_at = NOW() + INTERVAL '30 day',
        expert_review_weight = 100,
        candidate_review_weight = 0,
        updated_at = NOW()
    WHERE id = v_season_id;
  END IF;
END $$;

-- 2) team for submitted project
DO $$
DECLARE
  v_season_id BIGINT;
  v_leader_id BIGINT;
  v_team_id BIGINT;
BEGIN
  SELECT id INTO v_season_id
  FROM seasons
  WHERE title = '테스트 시즌'
  ORDER BY id DESC
  LIMIT 1;

  SELECT id INTO v_leader_id
  FROM users
  WHERE email = 'test2@restart-point.com';

  SELECT id INTO v_team_id
  FROM teams
  WHERE season_id = v_season_id
    AND name = 'E2E 심사 대상 팀'
  ORDER BY id DESC
  LIMIT 1;

  IF v_team_id IS NULL THEN
    INSERT INTO teams (
      name,
      description,
      season_id,
      leader_id,
      status,
      recruiting_planner,
      recruiting_uxui,
      recruiting_frontend,
      recruiting_backend,
      created_at,
      updated_at
    ) VALUES (
      'E2E 심사 대상 팀',
      '리뷰 E2E용 팀',
      v_season_id,
      v_leader_id,
      'SUBMITTED',
      false,
      false,
      false,
      false,
      NOW(),
      NOW()
    ) RETURNING id INTO v_team_id;
  ELSE
    UPDATE teams
    SET description = '리뷰 E2E용 팀',
        leader_id = v_leader_id,
        status = 'SUBMITTED',
        recruiting_planner = false,
        recruiting_uxui = false,
        recruiting_frontend = false,
        recruiting_backend = false,
        updated_at = NOW()
    WHERE id = v_team_id;
  END IF;

  INSERT INTO team_members (
    team_id,
    user_id,
    role,
    status,
    application_message,
    created_at,
    updated_at
  )
  SELECT
    v_team_id,
    u.id,
    'BACKEND',
    'ACCEPTED',
    'E2E 시드 멤버',
    NOW(),
    NOW()
  FROM users u
  WHERE u.email = 'review-target@restart-point.com'
    AND NOT EXISTS (
      SELECT 1
      FROM team_members tm
      WHERE tm.team_id = v_team_id
        AND tm.user_id = u.id
    );
END $$;

-- 3) submitted project visible in review page
DO $$
DECLARE
  v_team_id BIGINT;
  v_project_id BIGINT;
BEGIN
  SELECT t.id INTO v_team_id
  FROM teams t
  JOIN seasons s ON s.id = t.season_id
  WHERE s.title = '테스트 시즌'
    AND t.name = 'E2E 심사 대상 팀'
  ORDER BY t.id DESC
  LIMIT 1;

  SELECT id INTO v_project_id
  FROM projects
  WHERE team_id = v_team_id;

  IF v_project_id IS NULL THEN
    INSERT INTO projects (
      team_id,
      name,
      problem_definition,
      target_users,
      solution,
      ai_usage,
      figma_url,
      github_url,
      notion_url,
      demo_url,
      status,
      team_retrospective,
      created_at,
      updated_at
    ) VALUES (
      v_team_id,
      'AI 회고 도우미',
      '팀 회고 내용이 흩어져서 액션 아이템 추적이 어려운 문제',
      '부트캠프 프로젝트 팀',
      '회고를 구조화하고 후속 액션을 정리하는 웹 서비스',
      '회고 요약과 액션 아이템 추천에 AI 활용',
      'https://figma.com/file/e2e-review-seed',
      'https://github.com/spring-kang/restart-point',
      'https://notion.so/e2e-review-seed',
      'https://restart-point.com',
      'SUBMITTED',
      'E2E 리뷰 테스트용 제출 회고',
      NOW(),
      NOW()
    );
  ELSE
    UPDATE projects
    SET name = 'AI 회고 도우미',
        problem_definition = '팀 회고 내용이 흩어져서 액션 아이템 추적이 어려운 문제',
        target_users = '부트캠프 프로젝트 팀',
        solution = '회고를 구조화하고 후속 액션을 정리하는 웹 서비스',
        ai_usage = '회고 요약과 액션 아이템 추천에 AI 활용',
        figma_url = 'https://figma.com/file/e2e-review-seed',
        github_url = 'https://github.com/spring-kang/restart-point',
        notion_url = 'https://notion.so/e2e-review-seed',
        demo_url = 'https://restart-point.com',
        status = 'SUBMITTED',
        team_retrospective = 'E2E 리뷰 테스트용 제출 회고',
        updated_at = NOW()
    WHERE id = v_project_id;
  END IF;
END $$;

-- 4) optional: one historical review row for "내가 심사한 목록" UI
-- this uses a separate project so the main review target remains reviewable
DO $$
DECLARE
  v_season_id BIGINT;
  v_leader_id BIGINT;
  v_team_id BIGINT;
  v_project_id BIGINT;
  v_reviewer_id BIGINT;
  v_review_id BIGINT;
BEGIN
  SELECT id INTO v_season_id
  FROM seasons
  WHERE title = '테스트 시즌'
  ORDER BY id DESC
  LIMIT 1;

  SELECT id INTO v_leader_id
  FROM users
  WHERE email = 'review-target@restart-point.com';

  SELECT id INTO v_team_id
  FROM teams
  WHERE season_id = v_season_id
    AND name = 'E2E 이전 심사 팀'
  ORDER BY id DESC
  LIMIT 1;

  IF v_team_id IS NULL THEN
    INSERT INTO teams (
      name,
      description,
      season_id,
      leader_id,
      status,
      recruiting_planner,
      recruiting_uxui,
      recruiting_frontend,
      recruiting_backend,
      created_at,
      updated_at
    ) VALUES (
      'E2E 이전 심사 팀',
      '내가 심사한 목록 노출용 팀',
      v_season_id,
      v_leader_id,
      'REVIEWED',
      false,
      false,
      false,
      false,
      NOW(),
      NOW()
    ) RETURNING id INTO v_team_id;
  END IF;

  SELECT id INTO v_project_id
  FROM projects
  WHERE team_id = v_team_id;

  IF v_project_id IS NULL THEN
    INSERT INTO projects (
      team_id,
      name,
      problem_definition,
      target_users,
      solution,
      ai_usage,
      figma_url,
      github_url,
      notion_url,
      demo_url,
      status,
      team_retrospective,
      created_at,
      updated_at
    ) VALUES (
      v_team_id,
      '이전 심사 프로젝트',
      '이전 심사 목록 확인용 프로젝트',
      '운영 확인자',
      '심사 이력을 보여주기 위한 샘플 프로젝트',
      'AI 요약',
      'https://figma.com/file/e2e-review-history',
      'https://github.com/spring-kang/restart-point',
      'https://notion.so/e2e-review-history',
      'https://restart-point.com',
      'COMPLETED',
      '이전 심사 이력용 회고',
      NOW(),
      NOW()
    ) RETURNING id INTO v_project_id;
  END IF;

  SELECT id INTO v_reviewer_id
  FROM users
  WHERE email = 'test1@restart-point.com';

  SELECT id INTO v_review_id
  FROM reviews
  WHERE project_id = v_project_id
    AND reviewer_id = v_reviewer_id;

  IF v_review_id IS NULL THEN
    INSERT INTO reviews (
      project_id,
      reviewer_id,
      review_type,
      overall_comment,
      submitted_at,
      created_at,
      updated_at
    ) VALUES (
      v_project_id,
      v_reviewer_id,
      'EXPERT',
      '기본 시드 리뷰입니다.',
      NOW() - INTERVAL '1 day',
      NOW(),
      NOW()
    ) RETURNING id INTO v_review_id;

    INSERT INTO review_scores (review_id, rubric_item, score, comment, created_at, updated_at)
    VALUES
      (v_review_id, 'PROBLEM_DEFINITION', 4, '문제 정의가 비교적 명확합니다.', NOW(), NOW()),
      (v_review_id, 'USER_VALUE', 4, '사용자 가치가 잘 드러납니다.', NOW(), NOW()),
      (v_review_id, 'AI_USAGE', 5, 'AI 활용 포인트가 분명합니다.', NOW(), NOW()),
      (v_review_id, 'UX_COMPLETENESS', 4, 'UX 흐름이 자연스럽습니다.', NOW(), NOW()),
      (v_review_id, 'TECHNICAL_FEASIBILITY', 4, '구현 현실성이 높습니다.', NOW(), NOW()),
      (v_review_id, 'COLLABORATION', 4, '협업 구조가 잘 보입니다.', NOW(), NOW());
  END IF;
END $$;

COMMIT;

-- Optional cleanup examples
-- DELETE FROM review_scores WHERE review_id IN (SELECT id FROM reviews WHERE overall_comment = '기본 시드 리뷰입니다.');
-- DELETE FROM reviews WHERE overall_comment = '기본 시드 리뷰입니다.';
-- DELETE FROM projects WHERE name = 'AI 회고 도우미' AND team_id IN (SELECT id FROM teams WHERE name = 'E2E 심사 대상 팀');
-- DELETE FROM team_members WHERE application_message = 'E2E 시드 멤버';
-- DELETE FROM teams WHERE name = 'E2E 심사 대상 팀';
-- DELETE FROM seasons WHERE title = '테스트 시즌' AND description = 'E2E 리뷰 테스트용 시즌';
