CREATE TABLE IF NOT EXISTS `zstack`.`ReplayMessageVO` (
    `id`           BIGINT UNSIGNED NOT NULL UNIQUE AUTO_INCREMENT,
    `msgDump`      TEXT,
    `locationType` VARCHAR(256)    NOT NULL,
    `locationUuid` VARCHAR(32)     NOT NULL,
    `createDate`   TIMESTAMP       NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8;