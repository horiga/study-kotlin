-- noinspection SqlNoDataSourceInspectionForFile
-- noinspection SqlDialectInspectionForFile

SET CHARSET utf8mb4;

CREATE TABLE IF NOT EXISTS book (
  id                   VARCHAR(20)   PRIMARY KEY,
  name                 VARCHAR(200)  NOT NULL,
  registration_date    DATETIME      NOT NULL
) ENGINE=innodb DEFAULT CHARSET=utf8mb4;