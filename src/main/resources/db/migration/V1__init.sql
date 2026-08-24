CREATE TABLE IF NOT EXISTS `about`
(
    `id`          int         NOT NULL AUTO_INCREMENT,
    `paragraph`   longtext    NOT NULL,
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `core_skills`
(
    `id`          int         NOT NULL AUTO_INCREMENT,
    `summary`     varchar(255)         NOT NULL,
    `details`     longtext,
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `type_UNIQUE` (`summary`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `education`
(
    `id`          int          NOT NULL AUTO_INCREMENT,
    `start_date`  varchar(255) NOT NULL,
    `end_date`    varchar(255)          DEFAULT NULL,
    `institution` varchar(255) NOT NULL,
    `degree`      varchar(255)          DEFAULT NULL,
    `major`       varchar(255) NOT NULL,
    `location`    varchar(255) NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `key_responsibilities`
(
    `id`            int          NOT NULL AUTO_INCREMENT,
    `working_id`    int          NOT NULL,
    `responsibility` longtext,
    `title`         varchar(255) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `user`
(
    `id`          varchar(255) NOT NULL DEFAULT 'uuid()',
    `username`    varchar(255) NOT NULL,
    `password`    varchar(255) NOT NULL,
    `create_time` timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` timestamp    NULL     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `username_UNIQUE` (`username`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `working_experience`
(
    `id`          int          NOT NULL AUTO_INCREMENT,
    `working_id`  int          NOT NULL,
    `start_date`  varchar(45)  NOT NULL,
    `end_date`    varchar(45)           DEFAULT NULL,
    `job_title`   varchar(255) NOT NULL,
    `company`     varchar(255) NOT NULL,
    `location`    varchar(255) NOT NULL,
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `working_id_UNIQUE` (`working_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;