-- Backward-compatible additive migration.
-- Adding a NOT NULL column with a safe default keeps existing rows valid and lets an older
-- application version keep running against the new schema (expand/contract pattern).
ALTER TABLE operations ADD COLUMN priority INTEGER NOT NULL DEFAULT 3;
