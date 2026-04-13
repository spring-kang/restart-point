-- Candidate review cleanup for PostgreSQL
-- Removes legacy CANDIDATE reviews and their child review_scores.
-- Review carefully before running in production.

BEGIN;

DELETE FROM review_scores
WHERE review_id IN (
  SELECT id
  FROM reviews
  WHERE review_type = 'CANDIDATE'
);

DELETE FROM reviews
WHERE review_type = 'CANDIDATE';

COMMIT;
