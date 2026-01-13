-- ZSTAC-75319: Add normalizedModelName column for GPU spec dedup
CALL ADD_COLUMN('GpuDeviceSpecVO', 'normalizedModelName', 'VARCHAR(255)', 1, NULL);
CALL CREATE_INDEX('GpuDeviceSpecVO', 'idx_gpu_spec_normalized_model', 'normalizedModelName');

CREATE TABLE IF NOT EXISTS  `zstack`.`BareMetal2DpuChassisVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `config` TEXT  DEFAULT NULL,
    `hostUuid` varchar(32) DEFAULT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkBareMetal2DpuChassisVOChassisVO` FOREIGN KEY (`uuid`) REFERENCES `BareMetal2ChassisVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkBareMetal2DpuChassisVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE SET NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS  `zstack`.`BareMetal2DpuHostVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `chassisUuid` VARCHAR(32) NOT NULL,
    `vendorType` VARCHAR(255) NOT NULL,
    `url` VARCHAR(255) NOT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkBareMetal2DpuHostVOHostVO` FOREIGN KEY (`uuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkBareMetal2DpuHostVOChassisVO` FOREIGN KEY (`chassisUuid`) REFERENCES `BareMetal2DpuChassisVO` (`uuid`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`BareMetal2InstanceVO`
DROP FOREIGN KEY `fkBareMetal2InstanceVOGatewayVO`,
DROP FOREIGN KEY `fkBareMetal2InstanceVOGatewayVO1`;

ALTER TABLE `zstack`.`BareMetal2InstanceVO`
    ADD CONSTRAINT `fkBareMetal2InstanceVOGatewayVO`
        FOREIGN KEY (`gatewayUuid`)
        REFERENCES `HostEO` (`uuid`)
        ON DELETE SET NULL,
    ADD CONSTRAINT `fkBareMetal2InstanceVOGatewayVO1`
        FOREIGN KEY (`lastGatewayUuid`)
        REFERENCES `HostEO` (`uuid`)
        ON DELETE SET NULL;

