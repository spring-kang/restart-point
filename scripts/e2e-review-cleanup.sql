-- Review E2E cleanup for PostgreSQL
-- Removes data created by scripts/e2e-review-seed.sql
-- Review carefully before running in prod.

BEGIN;

DO $$
DECLARE
  v_target_team_id BIGINT;
  v_history_team_id BIGINT;
  v_target_project_id BIGINT;
  v_history_project_id BIGINT;
BEGIN
  SELECT t.id INTO v_target_team_id
  FROM teams t
  JOIN seasons s ON s.id = t.season_id
  WHERE s.title = '테스트 시즌'
    AND t.name = 'E2E 심사 대상 팀'
  ORDER BY t.id DESC
  LIMIT 1;

  SELECT t.id INTO v_history_team_id
  FROM teams t
  JOIN seasons s ON s.id = t.season_id
  WHERE s.title = '테스트 시즌'
    AND t.name = 'E2E 이전 심사 팀'
  ORDER BY t.id DESC
  LIMIT 1;

  SELECT id INTO v_target_project_id FROM projects WHERE team_id = v_target_team_id;
  SELECT id INTO v_history_project_id FROM projects WHERE team_id = v_history_team_id;

  DELETE FROM review_scores
  WHERE review_id IN (
    SELECT id FROM reviews WHERE project_id IN (v_target_project_id, v_history_project_id)
  );

  DELETE FROM reviews
  WHERE project_id IN (v_target_project_id, v_history_project_id)
     OR overall_comment = '기본 시드 리뷰입니다.';

  DELETE FROM projects
  WHERE id IN (v_target_project_id, v_history_project_id)
     OR (name IN ('AI 회고 도우미', '이전 심사 프로젝트')
         AND team_id IN (COALESCE(v_target_team_id, -1), COALESCE(v_history_team_id, -1)));

  DELETE FROM team_members
  WHERE team_id IN (COALESCE(v_target_team_id, -1), COALESCE(v_history_team_id, -1))
     OR application_message = 'E2E 시드 멤버';

  DELETE FROM teams
  WHERE id IN (COALESCE(v_target_team_id, -1), COALESCE(v_history_team_id, -1))
     OR name IN ('E2E 심사 대상 팀', 'E2E 이전 심사 팀');

  DELETE FROM seasons
  WHERE title = '테스트 시즌'
    AND description = 'E2E 리뷰 테스트용 시즌';

  DELETE FROM users
  WHERE email IN (
    'test1@restart-point.com',
    'test2@restart-point.com',
    'review-target@restart-point.com'
  )
    AND name IN ('E2E 리뷰어', 'E2E 팀장', 'E2E 일반유저');
END $$;

COMMIT;
