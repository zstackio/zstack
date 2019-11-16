CREATE TABLE IF NOT EXISTS `zstack`.`SNSSmsEndpointVO`
(
    `uuid` varchar(32) NOT NULL UNIQUE,
    PRIMARY KEY (`uuid`),
    CONSTRAINT fkSNSSmsEndpointVOSNSApplicationEndpointVO FOREIGN KEY (uuid) REFERENCES SNSApplicationEndpointVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`SNSSmsReceiverVO`
(
    `uuid`         varchar(32) NOT NULL UNIQUE,
    `phoneNumber`  varchar(24) NOT NULL,
    `endpointUuid` varchar(32) NOT NULL,
    `type`         varchar(24) NOT NULL,
    `description`  varchar(255) DEFAULT NULL,
    `lastOpDate`   timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate`   timestamp,
    PRIMARY KEY (`uuid`),
    CONSTRAINT fkSNSSmsReceiverVOSNSSmsEndpointVO FOREIGN KEY (endpointUuid) REFERENCES SNSSmsEndpointVO (uuid)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`AliyunSmsSNSTextTemplateVO`
(
    `uuid`              varchar(32) NOT NULL UNIQUE,
    `sign`              varchar(24) NOT NULL,
    `alarmTemplateCode` varchar(24) NOT NULL,
    `eventTemplateCode` varchar(24) NOT NULL,
    `eventTemplate`     text,
    PRIMARY KEY (`uuid`),
    CONSTRAINT fkAliyunSmsSNSTextTemplateVOSNSTextTemplateVO FOREIGN KEY (uuid) REFERENCES SNSTextTemplateVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

# Remove the unique constrain of name in HybridAccountVO, the second one is imported by someone on 3.4.0
ALTER TABLE HybridAccountVO
    DROP INDEX name;
ALTER TABLE HybridAccountVO
    DROP INDEX name_2;

CREATE TABLE IF NOT EXISTS `InstallPathRecycleVO` (
    `trashId` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `resourceUuid` varchar(32) NOT NULL,
    `resourceType` varchar(32) NOT NULL,
    `storageUuid` varchar(32) NOT NULL,
    `storageType` varchar(32) NOT NULL,
    `installPath` varchar(1024) NOT NULL,
    `hostUuid` varchar(32) DEFAULT NULL,
    `hypervisorType` varchar(32) DEFAULT NULL,
    `trashType` varchar(32) NOT NULL,
    `isFolder` boolean NOT NULL DEFAULT FALSE,
    `size` bigint unsigned NOT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`trashId`),
    UNIQUE KEY `trashId` (`trashId`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `zstack`.`VmPriorityConfigVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `level` VARCHAR(255) NOT NULL UNIQUE,
    `cpuShares` int NOT NULL,
    `oomScoreAdj` int NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `SNSEmailEndpointVO` modify column email varchar(1024) DEFAULT NULL;

CREATE TABLE IF NOT EXISTS `zstack`.`SNSEmailAddressVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `emailAddress` varchar(1024) NOT NULL,
    `endpointUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP PROCEDURE IF EXISTS upgradeEmailAddressFromEndpoint;

DELIMITER $$
CREATE PROCEDURE upgradeEmailAddressFromEndpoint()
    BEGIN
        DECLARE email_address varchar(1024);
        DECLARE endpoint_uuid varchar(32);
        DECLARE email_address_count INT DEFAULT 0;
        DECLARE email_address_uuid varchar(32);
        DECLARE done INT DEFAULT FALSE;
        DECLARE cur CURSOR FOR SELECT uuid, email FROM zstack.SNSEmailEndpointVO WHERE `email` IS NOT NULL;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO endpoint_uuid, email_address;
            IF done THEN
                LEAVE read_loop;
            END IF;

            SELECT count(*) INTO email_address_count FROM zstack.SNSEmailAddressVO WHERE emailAddress = email_address and endpointUuid = endpoint_uuid;

            IF (email_address_count = 0) THEN
                SET email_address_uuid = REPLACE(UUID(), '-', '');

                INSERT INTO ResourceVO (`uuid`, `resourceName`, `resourceType`, `concreteResourceType`)
                VALUES (email_address_uuid, NULL, 'SNSEmailAddressVO', 'org.zstack.sns.platform.email.SNSEmailAddressVO');

                INSERT INTO `SNSEmailAddressVO` (`uuid`, `emailAddress`, `endpointUuid`, `createDate`, `lastOpDate`)
                VALUES (email_address_uuid, email_address, endpoint_uuid, NOW(), NOW());
            END IF;
        END LOOP;
        CLOSE cur;
        SELECT CURTIME();
    END $$
DELIMITER ;

CALL upgradeEmailAddressFromEndpoint();
DROP PROCEDURE IF EXISTS upgradeEmailAddressFromEndpoint;

UPDATE zstack.SNSEmailEndpointVO SET email = NULL;

-- ----------------------------
--  For multicast router
-- ----------------------------
CREATE TABLE IF NOT EXISTS `zstack`.`MulticastRouterVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `description` VARCHAR(2048) DEFAULT NULL,
    `state` VARCHAR(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`MulticastRouterRendezvousPointVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `multicastRouterUuid` VARCHAR(32) NOT NULL,
    `rpAddress` VARCHAR(64) NOT NULL,
    `groupAddress` VARCHAR(64) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    CONSTRAINT fkMultiCastRouterRendezvousPointVOMulticastRouterVO FOREIGN KEY (multicastRouterUuid) REFERENCES MulticastRouterVO (uuid) ON DELETE CASCADE,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`MulticastRouterVpcVRouterRefVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `vpcRouterUuid` VARCHAR(32) NOT NULL,
    CONSTRAINT fkMulticastRouterVpcVRouterRefVOMulticastRouterVO FOREIGN KEY (uuid) REFERENCES MulticastRouterVO (uuid) ON DELETE CASCADE,
    CONSTRAINT fkMulticastRouterVpcVRouterRefVOVpcRouterVmVO FOREIGN KEY (vpcRouterUuid) REFERENCES VpcRouterVmVO (uuid) ON DELETE CASCADE,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX idxLongJobVOapiId ON LongJobVO (apiId);

ALTER TABLE `zstack`.`RoleVO` ADD COLUMN identity VARCHAR(64) DEFAULT NULL;

--  FOR GUEST TOOLS
CREATE TABLE IF NOT EXISTS `zstack`.`GuestToolsVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `name` varchar(255) DEFAULT "",
    `description` varchar(2048) DEFAULT NULL,
    `managementNodeUuid` VARCHAR(32) NOT NULL,
    `architecture` VARCHAR(32) NOT NULL,
    `hypervisorType` VARCHAR(32) NOT NULL,
    `version` VARCHAR(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `BillingResourceLabelVO` modify labelKey varchar(255) NOT NULL;

--  add h3c hardware sdn
-- ----------------------------
CREATE TABLE IF NOT EXISTS `SdnControllerVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `vendorType` VARCHAR(255) NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `ip` VARCHAR(255) NOT NULL,
    `username` VARCHAR(255) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  IF NOT EXISTS `HardwareL2VxlanNetworkPoolVO` (
  `uuid` varchar(32) NOT NULL UNIQUE,
  `sdnControllerUuid` VARCHAR(32) NOT NULL,
  PRIMARY KEY  (`uuid`),
  CONSTRAINT fkHardwareL2VxlanNetworkPoolVOL2NetworkEO FOREIGN KEY (uuid) REFERENCES L2NetworkEO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
--  For port mirror
-- ----------------------------
CREATE TABLE IF NOT EXISTS `zstack`.`PortMirrorVO` (
  `uuid` VARCHAR(32) NOT NULL,
  `name` VARCHAR(128) DEFAULT "",
  `state` VARCHAR(128) DEFAULT "Enable",
  `mirrorNetworkUuid` VARCHAR(32) NOT NULL,
  `description` VARCHAR(1024) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`) USING BTREE,
  CONSTRAINT `fkPortMirrorVOL3NetworkVO` FOREIGN KEY (`mirrorNetworkUuid`) REFERENCES `L3NetworkEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`PortMirrorSessionVO` (
  `uuid` VARCHAR(32) NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `srcEndPoint` VARCHAR(32) NOT NULL,
  `dstEndPoint` VARCHAR(32) NOT NULL,
  `type` VARCHAR(32) NOT NULL,
  `status` VARCHAR(128) DEFAULT 'Created',
  `internalId` int unsigned NOT NULL,
  `description` VARCHAR(1024) DEFAULT NULL,
  `portMirrorUuid` VARCHAR(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`) USING BTREE,
  CONSTRAINT `fkPortMirrorSessionVOPortMirrorVO` FOREIGN KEY (`portMirrorUuid`) REFERENCES `PortMirrorVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkPortMirrorSessionVOSrcNIcVmNicVO` FOREIGN KEY (`srcEndPoint`) REFERENCES `VmNicVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`PortMirrorSessionSequenceNumberVO` (
    `id` int unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`MirrorNetworkUsedIpVO` (
  `uuid` VARCHAR(32) NOT NULL,
  `hostUuid` VARCHAR(32) NOT NULL,
  `clusterUuid` VARCHAR(32) NOT NULL,
  `l3NetworkUuid` VARCHAR(32) NOT NULL,
  `description` VARCHAR(1024) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`) USING BTREE,
  CONSTRAINT `fkMirrorNetworkUsedIpVOL3NetworkEO` FOREIGN KEY (`l3NetworkUuid`) REFERENCES `L3NetworkEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkMirrorNetworkUsedIpVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkMirrorNetworkUsedIpVOClusterEO` FOREIGN KEY (`clusterUuid`) REFERENCES `ClusterEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`PortMirrorSessionMirrorNetworkRefVO` (
  `uuid` VARCHAR(32) NOT NULL,
  `sessionUuid` VARCHAR(32) NOT NULL,
  `srcTunnelUuid` VARCHAR(32) NOT NULL,
  `dstTunnelUuid` VARCHAR(32),
  `type` VARCHAR(32) DEFAULT 'GRE',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`) USING BTREE,
  CONSTRAINT `fkMirrorRefVOPortMirrorSessionVO` FOREIGN KEY (`sessionUuid`) REFERENCES `PortMirrorSessionVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkMirrorRefVOMirrorNetworkUsedIpVOSrc` FOREIGN KEY (`srcTunnelUuid`) REFERENCES `MirrorNetworkUsedIpVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkMirrorRefVOMirrorNetworkUsedIpVODst` FOREIGN KEY (`dstTunnelUuid`) REFERENCES `MirrorNetworkUsedIpVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table SNSTextTemplateVO add type varchar(255) DEFAULT "ALARM";
update SNSApplicationEndpointVO set type = "SYSTEM_HTTP" where name = "system-alarm-endpoint" and platformUuid = "02d24b9b0a7f4ee1846f15cda248ceb7" and type = "HTTP";


DROP PROCEDURE IF EXISTS addMissingResourceRef;
DELIMITER $$
CREATE PROCEDURE addMissingResourceRef()
    BEGIN
        DECLARE groupUuid VARCHAR(32);
        DECLARE accountUuid VARCHAR(32);
        DECLARE done INT DEFAULT FALSE;
        DECLARE cur CURSOR FOR SELECT sgroup.uuid FROM `zstack`.`VolumeSnapshotGroupVO` sgroup
        WHERE NOT EXISTS (SELECT resourceUuid FROM zstack.AccountResourceRefVO WHERE resourceUuid = sgroup.uuid);
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO groupUuid;
            IF done THEN
                LEAVE read_loop;
            END IF;

            SELECT aref.accountUuid INTO accountUuid FROM `zstack`.`VolumeSnapshotGroupRefVO` sref, `zstack`.`AccountResourceRefVO` aref
            WHERE sref.volumeSnapshotGroupUuid = groupUuid
                  AND sref.volumeType = 'Root'
                  AND sref.volumeSnapshotUuid = aref.resourceUuid;

            IF accountUuid IS NOT NULL THEN
                INSERT INTO `zstack`.`AccountResourceRefVO` (`resourceType`, `resourceUuid`, `accountUuid`, `ownerAccountUuid`, `concreteResourceType`, `permission`, `isShared`, `createDate`, `lastOpDate`)
                VALUES ('VolumeSnapshotGroupVO', groupUuid, accountUuid, accountUuid, 'org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO', 2, 0, NOW(), NOW());
            END IF;
        END LOOP;
        CLOSE cur;
        SELECT CURTIME();
    END $$
DELIMITER ;
CALL addMissingResourceRef();
DROP PROCEDURE IF EXISTS addMissingResourceRef;

DROP PROCEDURE IF EXISTS modifyHostXfsFragAlarmDefaultThreshold;

DELIMITER $$
CREATE PROCEDURE modifyHostXfsFragAlarmDefaultThreshold()
    BEGIN
        DECLARE done INT DEFAULT FALSE;
        DECLARE uuid VARCHAR(32);
        DECLARE threshold DOUBLE;
        DECLARE cur CURSOR FOR SELECT v.uuid,v.threshold FROM AlarmVO v WHERE v.uuid = "bf7359930ee444d286fb88d2e51acf51";
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO uuid,threshold;
            IF done THEN
                LEAVE read_loop;
            END IF;

            IF threshold != "85" THEN
                UPDATE AlarmVO v SET v.threshold = "85" WHERE v.uuid = uuid;
            END IF;
        END LOOP;
        CLOSE cur;
        SELECT CURTIME();
    END $$
DELIMITER ;

call modifyHostXfsFragAlarmDefaultThreshold();
DROP PROCEDURE IF EXISTS modifyHostXfsFragAlarmDefaultThreshold;