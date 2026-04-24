CREATE TABLE IF NOT EXISTS `HostCacheStoreVO` (
    `uuid`              VARCHAR(32)    NOT NULL,
    `hostUuid`          VARCHAR(32)    NOT NULL,
    `name`              VARCHAR(255)   DEFAULT NULL,
    `description`       VARCHAR(2048)  DEFAULT NULL,
    `mountPoint`        VARCHAR(255)   DEFAULT NULL,
    `devices`           TEXT,
    `state`             VARCHAR(32)    NOT NULL,
    `status`            VARCHAR(32)    NOT NULL,
    `createDate`        TIMESTAMP      NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate`        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkHostCacheStoreVOHostEO`
    FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`)
    ON DELETE CASCADE
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE IF NOT EXISTS `HostCacheStoreCapacityVO` (
    `uuid`              VARCHAR(32)    NOT NULL,
    `totalCapacity`     BIGINT         NOT NULL DEFAULT 0,
    `availableCapacity` BIGINT         NOT NULL DEFAULT 0,
    `allocated`         BIGINT         NOT NULL DEFAULT 0,
    `dirty`             BIGINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkHostCacheStoreCapacityVOHostCacheStoreVO`
    FOREIGN KEY (`uuid`) REFERENCES `HostCacheStoreVO` (`uuid`)
    ON DELETE CASCADE
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE IF NOT EXISTS `VolumeCacheVO` (
    `uuid`        VARCHAR(32)   NOT NULL,
    `volumeUuid`  VARCHAR(32)   NOT NULL,
    `poolUuid`    VARCHAR(32)   DEFAULT NULL,
    `installPath` VARCHAR(2048) DEFAULT NULL,
    `cacheMode`   VARCHAR(32)   NOT NULL,
    `status`      VARCHAR(32)   NOT NULL,
    `createDate`  TIMESTAMP     NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate`  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uniVolumeCacheVOVolumeUuid` (`volumeUuid`),
    CONSTRAINT `fkVolumeCacheVOVolumeEO`
    FOREIGN KEY (`volumeUuid`) REFERENCES `VolumeEO` (`uuid`)
    ON DELETE CASCADE,
    CONSTRAINT `fkVolumeCacheVOPoolUuid`
    FOREIGN KEY (`poolUuid`) REFERENCES `HostCacheStoreVO` (`uuid`)
    ON DELETE SET NULL
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8;
