CREATE TABLE IF NOT EXISTS `zstack`.`HaNetworkGroupVO` (
                                                           `uuid` VARCHAR(32) NOT NULL UNIQUE COMMENT 'uuid',
    `name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `type` VARCHAR(128) NOT NULL,
    `minAvailableCount` INT(10) NOT NULL DEFAULT 1,
    `state` VARCHAR(32) NOT NULL DEFAULT 'Enabled',
    `lastOpDate` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`HaNetworkGroupL3NetworkRefVO` (
                                                                       `uuid` VARCHAR(32) NOT NULL UNIQUE COMMENT 'uuid',
    `haNetworkGroupUuid` VARCHAR(32) NOT NULL,
    `l3NetworkUuid` VARCHAR(32) NOT NULL,
    `lastOpDate` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    INDEX `idxHaNetworkGroupL3NetworkRefVOhaNetworkGroupUuid` (`haNetworkGroupUuid`),
    UNIQUE INDEX `ukHaNetworkGroupL3NetworkRefVOl3NetworkUuid` (`l3NetworkUuid`),
    CONSTRAINT `fkHaNetworkGroupL3NetworkRefVOHaNetworkGroupVO` FOREIGN KEY (`haNetworkGroupUuid`) REFERENCES `zstack`.`HaNetworkGroupVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkHaNetworkGroupL3NetworkRefVOL3NetworkEO` FOREIGN KEY (`l3NetworkUuid`) REFERENCES `zstack`.`L3NetworkEO` (`uuid`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`HostHaNetworkGroupStatusVO` (
                                                                     `uuid` VARCHAR(32) NOT NULL UNIQUE COMMENT 'uuid',
    `hostUuid` VARCHAR(32) NOT NULL,
    `networkGroupUuid` VARCHAR(32) NOT NULL,
    `status` VARCHAR(32) NOT NULL DEFAULT 'Unknown',
    `lastOpDate` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    UNIQUE INDEX `ukHostHaNetworkGroupStatusVOHostUuidNetworkGroupUuid` (`hostUuid`, `networkGroupUuid`),
    INDEX `idxHostHaNetworkGroupStatusVOhostUuid` (`hostUuid`),
    INDEX `idxHostHaNetworkGroupStatusVOnetworkGroupUuid` (`networkGroupUuid`),
    CONSTRAINT `fkHostHaNetworkGroupStatusVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `zstack`.`HostEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkHostHaNetworkGroupStatusVOHaNetworkGroupVO` FOREIGN KEY (`networkGroupUuid`) REFERENCES `zstack`.`HaNetworkGroupVO` (`uuid`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`HaNetworkGroupGlobalConfigVersionVO` (
                                                                              `name` VARCHAR(64) NOT NULL,
    `version` BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (`name`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT IGNORE INTO `zstack`.`HaNetworkGroupGlobalConfigVersionVO` (`name`, `version`)
VALUES ('ha-network-group', 0);
