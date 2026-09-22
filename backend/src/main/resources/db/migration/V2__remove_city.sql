-- V2: remove city column (manually removed on existing DBs).
-- Makes fresh DBs consistent with the JPA entity which no longer has city.
-- Idempotent: handles case where column/index already removed manually.

SET @has_city := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'city');

SET @sql := IF(@has_city > 0, 'ALTER TABLE users DROP COLUMN city', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Drop index if it still exists separately (normally dropped with column)
SET @has_idx := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND INDEX_NAME = 'idx_users_city');

SET @sql2 := IF(@has_idx > 0, 'DROP INDEX idx_users_city ON users', 'SELECT 1');
PREPARE stmt2 FROM @sql2; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;
