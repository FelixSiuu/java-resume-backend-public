-- V2__add_user_id.sql（冪等版本）

-- 對每個表檢查 user_id 是否存在，如果不存在才新增
-- 使用 MySQL 的存儲過程方式

SET @dbname = DATABASE();

-- 對 about 表
SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @dbname
       AND TABLE_NAME = 'about'
       AND COLUMN_NAME = 'user_id') = 0,
    'ALTER TABLE `about` ADD COLUMN `user_id` VARCHAR(255) NOT NULL DEFAULT ''user'' AFTER `id`;',
    'SELECT 1;'
));
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 對 core_skills 表
SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @dbname
       AND TABLE_NAME = 'core_skills'
       AND COLUMN_NAME = 'user_id') = 0,
    'ALTER TABLE `core_skills` ADD COLUMN `user_id` VARCHAR(255) NOT NULL DEFAULT ''user'' AFTER `id`;',
    'SELECT 1;'
));
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 對 education 表
SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @dbname
       AND TABLE_NAME = 'education'
       AND COLUMN_NAME = 'user_id') = 0,
    'ALTER TABLE `education` ADD COLUMN `user_id` VARCHAR(255) NOT NULL DEFAULT ''user'' AFTER `id`;',
    'SELECT 1;'
));
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 對 key_responsibilities 表
SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @dbname
       AND TABLE_NAME = 'key_responsibilities'
       AND COLUMN_NAME = 'user_id') = 0,
    'ALTER TABLE `key_responsibilities` ADD COLUMN `user_id` VARCHAR(255) NOT NULL DEFAULT ''user'' AFTER `id`;',
    'SELECT 1;'
));
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 對 working_experience 表
SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @dbname
       AND TABLE_NAME = 'working_experience'
       AND COLUMN_NAME = 'user_id') = 0,
    'ALTER TABLE `working_experience` ADD COLUMN `user_id` VARCHAR(255) NOT NULL DEFAULT ''user'' AFTER `id`;',
    'SELECT 1;'
));
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;