-- noinspection SqlNoDataSourceInspectionForFile
-- noinspection SqlDialectInspectionForFile

SET CHARSET utf8mb4;

CREATE TABLE IF NOT EXISTS `book` (
  id                   VARCHAR(20)   PRIMARY KEY,
  name                 VARCHAR(200)  NOT NULL,
  publisher_id         VARCHAR(20)   NOT NULL,
  registration_date    DATETIME      NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `publisher` (
  id                   VARCHAR(20)   PRIMARY KEY,
  name                 VARCHAR(200)  NOT NULL,
  registration_date    DATETIME      NOT NULL,
  UNIQUE KEY uk_name (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `sample` (
  `id`                 BINARY(16) NOT NULL,
  `type`               VARCHAR(3) NOT NULL,
  `enabled`            TINYINT(1) NOT NULL,
  `number`             INT,
  `memo`               VARCHAR(200),
  `registration_date`  DATETIME NOT NULL,
  PRIMARY KEY (`id`)
)
  ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4;
