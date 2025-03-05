DROP PROCEDURE IF EXISTS createThickProvisionVolumeTag;
DELIMITER $$
CREATE PROCEDURE createThickProvisionVolumeTag()
BEGIN
    DECLARE volUuid VARCHAR(32);
    DECLARE newTagUuid VARCHAR(32);
    DECLARE done INT DEFAULT FALSE;

    DECLARE volCursor CURSOR FOR
        SELECT uuid
        FROM zstack.VolumeVO
        WHERE type = 'Memory'
        AND primaryStorageUuid IN (
            SELECT uuid
            FROM zstack.PrimaryStorageVO
            WHERE type = 'SharedBlock'
        )
        AND uuid NOT IN (
            SELECT resourceUuid
            FROM zstack.SystemTagVO
            WHERE resourceType = 'VolumeVO'
            AND tag = 'volumeProvisioningStrategy::ThickProvisioning'
        );

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN volCursor;

    read_loop:
    LOOP
        FETCH volCursor INTO volUuid;
        IF done THEN
            LEAVE read_loop;
        END IF;

        SET newTagUuid = REPLACE(UUID(), '-', '');

        INSERT INTO zstack.SystemTagVO (uuid, resourceUuid, resourceType, inherent, type, tag, createDate, lastOpDate)
        VALUES (newTagUuid, volUuid, 'VolumeVO', 0, 'System', 'volumeProvisioningStrategy::ThickProvisioning', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
    END LOOP;

    CLOSE volCursor;
    SELECT CURTIME() AS finishTime;
END $$
DELIMITER ;

CALL createThickProvisionVolumeTag();
DROP PROCEDURE IF EXISTS createThickProvisionVolumeTag;

CREATE TABLE `zstack`.`ObservabilityServerOfferingVO`(
    `uuid`                  varchar(32) NOT NULL UNIQUE,
    `managementNetworkUuid` varchar(32) DEFAULT NULL,
    `publicNetworkUuid`     varchar(32) DEFAULT NULL,
    `imageUuid`             varchar(32) NOT NULL,
    `zoneUuid`              varchar(32) NOT NULL,
    `isDefault`             tinyint(1) unsigned DEFAULT 0,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE ObservabilityServerOfferingVO ADD CONSTRAINT fkObservabilityServerOfferingVOImageEO FOREIGN KEY (imageUuid) REFERENCES ImageEO (uuid) ON DELETE CASCADE;
ALTER TABLE ObservabilityServerOfferingVO ADD CONSTRAINT fkObservabilityServerOfferingVOInstanceOfferingEO FOREIGN KEY (uuid) REFERENCES InstanceOfferingEO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;
ALTER TABLE ObservabilityServerOfferingVO ADD CONSTRAINT fkObservabilityServerOfferingVOL3NetworkEO FOREIGN KEY (managementNetworkUuid) REFERENCES L3NetworkEO (uuid) ON DELETE CASCADE;
ALTER TABLE ObservabilityServerOfferingVO ADD CONSTRAINT fkObservabilityServerOfferingVOL3NetworkEO1 FOREIGN KEY (publicNetworkUuid) REFERENCES L3NetworkEO (uuid) ON DELETE CASCADE;
ALTER TABLE ObservabilityServerOfferingVO ADD CONSTRAINT fkObservabilityServerOfferingVOZoneEO FOREIGN KEY (zoneUuid) REFERENCES ZoneEO (uuid) ON DELETE CASCADE;

CREATE TABLE  `zstack`.`ObservabilityServerVmVO` (
   `uuid` varchar(32) NOT NULL UNIQUE,
   `publicNetworkUuid` varchar(32) DEFAULT NULL,
   PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE ObservabilityServerVmVO ADD CONSTRAINT fkObservabilityServerVmVOVmInstanceEO FOREIGN KEY (uuid) REFERENCES VmInstanceEO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;

CREATE TABLE `zstack`.`ObservabilityServerServiceRefVO`(
    `id`                              BIGINT UNSIGNED NOT NULL UNIQUE AUTO_INCREMENT,
    `observabilityServerOfferingUuid` varchar(32)          DEFAULT NULL,
    `observabilityServerUuid`         varchar(32) NOT NULL,
    `serviceUuid`                     varchar(32) NOT NULL,
    `serviceType`                     varchar(32) NOT NULL,
    `observabilityServerPublicIp`     varchar(32)          DEFAULT NULL,
    `servicePublicIp`                 varchar(32)          DEFAULT NULL,
    `lastOpDate`                      timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate`                      timestamp   NOT NULL DEFAULT '0000-00-00 00:00:00',
PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE ObservabilityServerServiceRefVO ADD CONSTRAINT fkObservabilityServerServiceRefVOResourceVO FOREIGN KEY (serviceUuid) REFERENCES ResourceVO (uuid) ON DELETE CASCADE;

CREATE TABLE IF NOT EXISTS `zstack`.`LogServerVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) NULL,
    `category` varchar(255) NOT NULL,
    `type` varchar(255) NOT NULL,
    `level` varchar(255) NULL,
    `configuration` text NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
