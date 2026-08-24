-- V3__drop_working_id.sql（冪等版本）

SET @dbname = DATABASE();

-- 1. 刪除索引（如果存在）
SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
     WHERE TABLE_SCHEMA = @dbname
       AND TABLE_NAME = 'working_experience'
       AND INDEX_NAME = 'working_id_UNIQUE') > 0,
    'ALTER TABLE `working_experience` DROP INDEX `working_id_UNIQUE`;',
    'SELECT 1;'
));
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 刪除欄位（如果存在）
SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @dbname
       AND TABLE_NAME = 'working_experience'
       AND COLUMN_NAME = 'working_id') > 0,
    'ALTER TABLE `working_experience` DROP COLUMN `working_id`;',
    'SELECT 1;'
));
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;