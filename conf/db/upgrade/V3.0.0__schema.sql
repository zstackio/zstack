ALTER TABLE `CaptchaVO` DROP COLUMN `attempts`;

CREATE TABLE `LoginAttemptsVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `targetResourceIdentity` VARCHAR(256) NOT NULL,
    `attempts` int(10) unsigned DEFAULT 0,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `DatabaseBackupVO` (
    `uuid`        VARCHAR(32)     NOT NULL,
    `name`        VARCHAR(255)    NOT NULL,
    `description` VARCHAR(2048)   DEFAULT NULL,
    `size`        BIGINT UNSIGNED NOT NULL,
    `state`       VARCHAR(64)     NOT NULL,
    `status`      VARCHAR(64)     NOT NULL,
    `metadata`    TEXT            DEFAULT NULL,
    `lastOpDate`  TIMESTAMP       NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate`  TIMESTAMP       NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# Foreign keys for table DatabaseBackupVO
ALTER TABLE DatabaseBackupVO ADD CONSTRAINT fkDatabaseBackupVOResourceVO FOREIGN KEY (uuid) REFERENCES ResourceVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;

CREATE TABLE IF NOT EXISTS `DatabaseBackupStorageRefVO` (
    `id`                 BIGINT UNSIGNED NOT NULL UNIQUE AUTO_INCREMENT,
    `backupStorageUuid`  VARCHAR(32)     NOT NULL,
    `databaseBackupUuid` VARCHAR(32)     NOT NULL,
    `status`             VARCHAR(64)     NOT NULL,
    `installPath`        VARCHAR(2048)   NOT NULL,
    `exportUrl`          VARCHAR(255)    DEFAULT NULL,
    `lastOpDate`         TIMESTAMP       NOT NULL        DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate`         TIMESTAMP       NOT NULL        DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# Foreign keys for table DatabaseBackupStorageRefVO
ALTER TABLE DatabaseBackupStorageRefVO ADD CONSTRAINT fkDatabaseBackupStorageRefVOBackupStorageEO FOREIGN KEY (backupStorageUuid) REFERENCES BackupStorageEO (uuid) ON DELETE CASCADE;
ALTER TABLE DatabaseBackupStorageRefVO ADD CONSTRAINT fkDatabaseBackupStorageRefVODatabaseBackupVO FOREIGN KEY (databaseBackupUuid) REFERENCES DatabaseBackupVO (uuid) ON DELETE CASCADE;
