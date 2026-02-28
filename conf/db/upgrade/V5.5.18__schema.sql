CREATE TABLE IF NOT EXISTS `VmLocalVolumeCachePoolVO` (
    `uuid`              VARCHAR(32)    NOT NULL,
    `hostUuid`          VARCHAR(32)    NOT NULL,
    `name`              VARCHAR(255)   DEFAULT NULL,
    `description`       VARCHAR(2048)  DEFAULT NULL,
    `metadata`          VARCHAR(2048)  DEFAULT NULL,
    `state`             VARCHAR(32)    NOT NULL,
    `status`            VARCHAR(32)    NOT NULL,
    `createDate`        TIMESTAMP      NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate`        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkVmLocalVolumeCachePoolVOHostEO`
    FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`)
    ON DELETE CASCADE
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE IF NOT EXISTS `VmLocalVolumeCachePoolCapacityVO` (
    `uuid`              VARCHAR(32)    NOT NULL,
    `totalCapacity`     BIGINT         NOT NULL DEFAULT 0,
    `availableCapacity` BIGINT         NOT NULL DEFAULT 0,
    `allocated`         BIGINT         NOT NULL DEFAULT 0,
    `dirty`             BIGINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkVmLocalVolumeCachePoolCapacityVOVmLocalVolumeCachePoolVO`
    FOREIGN KEY (`uuid`) REFERENCES `VmLocalVolumeCachePoolVO` (`uuid`)
    ON DELETE CASCADE
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE IF NOT EXISTS `VmLocalVolumeCacheVO` (
    `uuid`        VARCHAR(32)   NOT NULL,
    `volumeUuid`  VARCHAR(32)   NOT NULL,
    `poolUuid`    VARCHAR(32)   DEFAULT NULL,
    `installPath` VARCHAR(2048) DEFAULT NULL,
    `cacheMode`   VARCHAR(32)   NOT NULL,
    `state`       VARCHAR(32)   NOT NULL,
    `createDate`  TIMESTAMP     NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate`  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uniVmLocalVolumeCacheVOVolumeUuid` (`volumeUuid`),
    CONSTRAINT `fkVmLocalVolumeCacheVOVolumeEO`
    FOREIGN KEY (`volumeUuid`) REFERENCES `VolumeEO` (`uuid`)
    ON DELETE CASCADE,
    CONSTRAINT `fkVmLocalVolumeCacheVOPoolUuid`
    FOREIGN KEY (`poolUuid`) REFERENCES `VmLocalVolumeCachePoolVO` (`uuid`)
    ON DELETE SET NULL
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8;
