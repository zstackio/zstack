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
