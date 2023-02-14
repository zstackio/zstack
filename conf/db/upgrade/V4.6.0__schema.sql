ALTER TABLE `zstack`.`HostNetworkBondingVO` ADD COLUMN `type` char(32) DEFAULT 'unknown';

DROP PROCEDURE IF EXISTS addLongJobVOIndex;

DELIMITER $$
CREATE PROCEDURE addLongJobVOIndex()
BEGIN
    IF NOT EXISTS (SELECT * FROM information_schema.statistics WHERE table_schema = 'zstack' AND table_name = "LongJobVO" AND index_name = "idxLongJobVOtargetResourceUuid") THEN
        CREATE INDEX idxLongJobVOtargetResourceUuid ON LongJobVO (targetResourceUuid);
    END IF;
END $$
DELIMITER ;

CALL addLongJobVOIndex();

CREATE TABLE IF NOT EXISTS `zstack`.`VmVdpaNicVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `pciDeviceUuid` varchar(32) DEFAULT NULL,
    `lastPciDeviceUuid` varchar(32) DEFAULT NULL,
    `srcPath` varchar(128) DEFAULT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkVmVdpaNicVOPciDeviceVO` FOREIGN KEY (`pciDeviceUuid`) REFERENCES `zstack`.`PciDeviceVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`VipNetworkServicesRefVO` DROP INDEX `uuid`;
ALTER TABLE `zstack`.`VipNetworkServicesRefVO` DROP PRIMARY KEY, ADD PRIMARY KEY(`uuid`,`serviceType`,`vipUuid`);

UPDATE `zstack`.`GlobalConfigVO` SET value="enable", defaultValue="enable" WHERE category="storageDevice" AND name="enable.multipath" AND value="true";
UPDATE `zstack`.`GlobalConfigVO` SET value="ignore", defaultValue="enable" WHERE category="storageDevice" AND name="enable.multipath" AND value="false";

ALTER TABLE `zstack`.`PriceVO` MODIFY COLUMN price DOUBLE(14,5);

CREATE TABLE IF NOT EXISTS `zstack`.`VmSchedulingRuleVO`(
    `uuid` varchar(32) not null unique,
    `rule` varchar (64) not null,
    `mode` varchar (64) not null,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkVmSchedulingRuleVOAffinityGroupVO FOREIGN KEY (uuid) REFERENCES AffinityGroupVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`VmSchedulingRuleGroupVO`(
    `uuid` varchar(32) not null unique,
    `name` varchar(255) not null,
    `appliance` varchar(128) not null,
    `zoneUuid` varchar(32) DEFAULT null,
    `description` varchar(2048) DEFAULT NULL,
    `srcUuid` varchar(255) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`VmSchedulingRuleGroupRefVO`(
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `vmGroupUuid` varchar(32) not null,
    `vmUuid` varchar(32) not null,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`id`),
    UNIQUE KEY `vmGroupUuid_vmUuid` (`vmGroupUuid`, `vmUuid`) USING BTREE,
    CONSTRAINT `fkVmSchedulingPolicyGroupRefVO` FOREIGN KEY (`vmGroupUuid`) REFERENCES `VmSchedulingRuleGroupVO`(`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `fkVmInstanceVORefVO` FOREIGN KEY (`vmUuid`) REFERENCES `VmInstanceEO`(`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`HostSchedulingRuleGroupVO`(
    `uuid` varchar(32) not null unique,
    `name` varchar(255) not null,
    `description` varchar(2048) DEFAULT NULL,
    `zoneUuid` varchar(32) not null,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`HostSchedulingRuleGroupRefVO`(
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `hostGroupUuid` varchar(32) not null,
    `hostUuid` varchar(32) not null,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`id`),
    UNIQUE KEY `hostGroupUuid_hostUuid` (`hostGroupUuid`, `hostUuid`) USING BTREE,
    CONSTRAINT `fkHostSchedulingRuleGroupRefVO` FOREIGN KEY (`hostGroupUuid`) REFERENCES `HostSchedulingRuleGroupVO`(`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `fkHostVORefVO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO`(`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`VmSchedulingRuleRefVO`(
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `vmSchedulingRuleUuid` varchar(32) NOT NULL,
    `vmGroupUuid` varchar(32) NOT NULL,
    `hostGroupUuid` varchar(32) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`id`),
    UNIQUE KEY `ruleUuid_vmGroupUuid_hostGroupUuid` (`vmSchedulingRuleUuid`, `vmGroupUuid`, `hostGroupUuid`) USING BTREE,
    CONSTRAINT `fkVmSchedulingRuleVORefVO` FOREIGN KEY (`vmSchedulingRuleUuid`) REFERENCES `VmSchedulingRuleVO`(`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `fkVmSchedulingRuleGroupVORefVO` FOREIGN KEY (`vmGroupUuid`) REFERENCES `VmSchedulingRuleGroupVO`(`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `fkHostSchedulingRuleGroupVORefVO` FOREIGN KEY (`hostGroupUuid`) REFERENCES `HostSchedulingRuleGroupVO`(`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table `zstack`.`AffinityGroupVO` add column `zoneUuid` varchar(32) default null;

DELIMITER $$
CREATE PROCEDURE addZoneUuidOnAffinityGroupVO()
    BEGIN
        DECLARE agUuid VARCHAR(32);
        DECLARE done INT DEFAULT FALSE;
        DECLARE agZoneUuid VARCHAR(32);
        DECLARE vmCursor CURSOR FOR SELECT uuid name from `zstack`.`AffinityGroupVO`;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
        OPEN vmCursor;
        read_loop: LOOP
            FETCH vmCursor INTO agUuid;
            IF done THEN
                LEAVE read_loop;
            END IF;

            select zoneUuid INTO agZoneUuid from VmInstanceVO where uuid in (select resourceUuid from AffinityGroupUsageVO where affinityGroupUuid = agUuid) group by zoneUuid order by count(zoneUuid) desc limit 1;
            IF agZoneUuid is null THEN
                select uuid into agZoneUuid from ZoneVO limit 1;
            END IF;

            UPDATE `zstack`.`AffinityGroupVO` SET `zoneUuid`=agZoneUuid WHERE `uuid`= agUuid;
        END LOOP;
        CLOSE vmCursor;
        SELECT CURTIME();
    END $$
DELIMITER ;

call addZoneUuidOnAffinityGroupVO();
DROP PROCEDURE IF EXISTS addZoneUuidOnAffinityGroupVO;

DELIMITER $$
CREATE PROCEDURE createVmSchedulingRule()
    BEGIN
        DECLARE agUuid VARCHAR(32);
        DECLARE done INT DEFAULT FALSE;
        DECLARE policyType VARCHAR(128);
        DECLARE ruleType VARCHAR(64);
        DECLARE ruleLevel VARCHAR(64);
        DECLARE ruleZoneUuid VARCHAR(32);
        DECLARE ruleAppliance VARCHAR(128);
        DECLARE ruleName VARCHAR(255);
        DECLARE ruleAccountUuid VARCHAR(32);
        DECLARE vmRuleGroupUuid VARCHAR(32);
        DECLARE vmCursor CURSOR FOR SELECT uuid, policy, zoneUuid, appliance, name from `zstack`.`AffinityGroupVO` order by createDate desc;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

        OPEN vmCursor;
        read_loop: LOOP
            FETCH vmCursor INTO agUuid, policyType, ruleZoneUuid, ruleAppliance, ruleName;
            IF done THEN
                LEAVE read_loop;
            END IF;

            IF (policyType = "AFFINITYSOFT") THEN
                set ruleType = "AFFINITY";
                set ruleLevel = "SOFT";
            ELSEIF (policyType = "AFFINITYHARD") THEN
                set ruleType = "AFFINITY";
                set ruleLevel = "HARD";
            ELSEIF (policyType = "ANTISOFT") THEN
                set ruleType = "ANTIAFFINITY";
                set ruleLevel ="SOFT";
            ELSE
                set ruleType = "ANTIAFFINITY";
                set ruleLevel ="HARD";
            END IF;
            set vmRuleGroupUuid = REPLACE(UUID(),'-','');

            INSERT INTO `zstack`.`VmSchedulingRuleVO`(`uuid`, `rule`, `mode`) VALUES (agUuid, ruleType, ruleLevel);
            INSERT INTO `zstack`.`VmSchedulingRuleGroupVO`(`uuid`, `name`, `appliance`, `zoneUuid`, `srcUuid`, `lastOpDate`,`createDate`)
            VALUES (vmRuleGroupUuid, ruleName, ruleAppliance, ruleZoneUuid, agUuid, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
            INSERT INTO  `zstack`.`ResourceVO` (`uuid`, `resourceName`, `resourceType`, `concreteResourceType`) VALUES (vmRuleGroupUuid, ruleName, 'VmSchedulingRuleGroupVO', 'org.zstack.header.vmscheduling.VmSchedulingRuleGroupVO');
            select accountUuid INTO ruleAccountUuid from AccountResourceRefVO where resourceUuid = agUuid and resourceType = 'AffinityGroupVO';
            INSERT INTO `zstack`.`VmSchedulingRuleRefVO`(`vmGroupUuid`, `vmSchedulingRuleUuid`, `lastOpDate`,`createDate`) VALUES (vmRuleGroupUuid, agUuid, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
            IF (ruleName <> 'zstack.affinity.group.for.virtual.router' and ruleAccountUuid is not null) THEN
                INSERT INTO `zstack`.`AccountResourceRefVO` ( `accountUuid`, `ownerAccountUuid`, `resourceUuid`, `resourceType`, `permission`, `isShared`, `lastOpDate`, `createDate`, `concreteResourceType`) VALUES (ruleAccountUuid, ruleAccountUuid,  vmRuleGroupUuid, 'VmSchedulingRuleGroupVO', 2, 0, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 'org.zstack.header.vmscheduling.VmSchedulingRuleGroupVO');
            END IF;
        END LOOP;
        CLOSE vmCursor;
        SELECT CURTIME();
    END $$
DELIMITER ;

call createVmSchedulingRule();
DROP PROCEDURE IF EXISTS createVmSchedulingRule;

DELIMITER $$
CREATE PROCEDURE createVmSchedulingRuleGroupRef()
    BEGIN
        DECLARE agUuid VARCHAR(32);
        DECLARE done INT DEFAULT FALSE;
        DECLARE vmUuid VARCHAR(128);
        DECLARE vmGroupUUid VARCHAR(32);
        DECLARE vmCursor CURSOR FOR SELECT affinityGroupUuid, resourceUuid name from `zstack`.`AffinityGroupUsageVO`;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

        OPEN vmCursor;
        read_loop: LOOP
            FETCH vmCursor INTO agUuid, vmUuid;
            IF done THEN
                LEAVE read_loop;
            END IF;

            select uuid INTO vmGroupUUid from VmSchedulingRuleGroupVO where srcUuid = agUuid;
            INSERT INTO `zstack`.`VmSchedulingRuleGroupRefVO`(`vmGroupUuid`, `vmUuid`, `lastOpDate`,`createDate`) VALUES(vmGroupUUid, vmUuid, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
        END LOOP;
        CLOSE vmCursor;
        SELECT CURTIME();
    END $$
DELIMITER ;

call createVmSchedulingRuleGroupRef();
DROP PROCEDURE IF EXISTS createVmSchedulingRuleGroupRef;

DELIMITER $$
CREATE PROCEDURE createAutoScalingVmTemplate()
    BEGIN
        DECLARE agUuid VARCHAR(32);
        DECLARE vmGroupUUid VARCHAR(32);
        DECLARE autoReleaseTagUuid VARCHAR(32);
        DECLARE autoScalingVmTemplateUuid VARCHAR(32);
        DECLARE done INT DEFAULT FALSE;
        DECLARE vmCursor CURSOR FOR select SUBSTRING(tag, 20), resourceUuid from SystemTagVO where resourceType ='AutoScalingVmTemplateVO' and tag like 'affinityGroupUuid::%';
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

        OPEN vmCursor;
        read_loop: LOOP
            FETCH vmCursor INTO agUuid, autoScalingVmTemplateUuid;
            IF done THEN
                LEAVE read_loop;
            END IF;

            SET autoReleaseTagUuid = REPLACE(UUID(), '-', '');
            select uuid into vmGroupUUid  from VmSchedulingRuleGroupVO where srcUuid = agUuid;
            INSERT INTO `zstack`.`SystemTagVO`(`uuid`, `resourceUuid`, `resourceType`, `inherent`, `type`, `tag`, `createDate`, `lastOpDate`)
            VALUES(autoReleaseTagUuid, autoScalingVmTemplateUuid, 'AutoScalingVmTemplateVO', 0, 'System', concat('vmSchedulingRuleGroupUuid::', vmGroupUUid),  CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
        END LOOP;
        CLOSE vmCursor ;
        SELECT CURTIME();
    END $$
DELIMITER ;

call createAutoScalingVmTemplate();
DROP PROCEDURE IF EXISTS createAutoScalingVmTemplate;

CREATE TABLE IF NOT EXISTS `zstack`.`DirectoryVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(256) NOT NULL,
    `groupName` varchar(2048) NOT NULL COMMENT 'equivalent to a path',
    `parentUuid` varchar(32),
    `rootDirectoryUuid` varchar(32) NOT NULL,
    `zoneUuid` varchar(32) NOT NULL,
    `type` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkDirectoryVOZoneEO` FOREIGN KEY (`zoneUuid`) REFERENCES `zstack`.`ZoneEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ResourceDirectoryRefVO` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `resourceUuid` varchar(32) NOT NULL,
    `directoryUuid` varchar(32) NOT NULL,
    `resourceType` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`id`),
    UNIQUE KEY `id` (`id`),
    KEY `fkResourceDirectoryRefVOResourceVO` (`resourceUuid`),
    KEY `fkResourceDirectoryRefVODirectoryVO` (`directoryUuid`),
    CONSTRAINT `fkResourceDirectoryRefVO` FOREIGN KEY (`resourceUuid`) REFERENCES `ResourceVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkResourceDirectoryRefVO1` FOREIGN KEY (`directoryUuid`) REFERENCES `DirectoryVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE SlbOfferingVO ADD CONSTRAINT fkSlbOfferingVOInstanceOfferingEO FOREIGN KEY (uuid) REFERENCES InstanceOfferingEO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;
