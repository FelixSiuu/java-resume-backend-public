ALTER TABLE `email_verification_token`
ADD COLUMN `purpose` VARCHAR(32) NOT NULL DEFAULT 'VERIFY'
COMMENT 'VERIFY | RESET' AFTER `email`,
RENAME TO `email_token`;