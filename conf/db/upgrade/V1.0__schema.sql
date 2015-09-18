CREATE TABLE  `zstack`.`ConvergedOfferingVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `networkBandwidth` bigint unsigned DEFAULT NULL,
    `volumeBandwidth` bigint unsigned DEFAULT NULL,
    `diskOfferingUuid` varchar(32) NOT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE HostCapacityVO ADD totalPhysicalMemory bigint unsigned NOT NULL DEFAULT 0;
ALTER TABLE HostCapacityVO ADD availablePhysicalMemory bigint unsigned NOT NULL DEFAULT 0;
