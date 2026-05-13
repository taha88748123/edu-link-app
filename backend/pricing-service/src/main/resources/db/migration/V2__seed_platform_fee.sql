-- ============================================================================
-- Seed the default STANDARD platform fee (10%). Idempotent.
-- ============================================================================

INSERT INTO platform_fees (id, name, percent, fixed_minor, active, valid_from, created_at)
SELECT gen_random_uuid(), 'STANDARD', 10.0, 0, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM platform_fees WHERE name = 'STANDARD');
