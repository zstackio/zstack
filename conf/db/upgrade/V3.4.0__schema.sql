ALTER TABLE `zstack`.`TwoFactorAuthenticationSecretVO` ADD COLUMN `status` varchar(255) NOT NULL DEFAULT "NewCreated";
INSERT IGNORE INTO ResourceVO (uuid, resourceType) SELECT t.uuid, "TwoFactorAuthenticationSecretVO" FROM TwoFactorAuthenticationSecretVO t;
ALTER TABLE `zstack`.`TwoFactorAuthenticationSecretVO` CHANGE `resourceUuid` `userUuid` VARCHAR(32) NOT NULL;
ALTER TABLE `zstack`.`TwoFactorAuthenticationSecretVO` CHANGE `resourceType` `userType` VARCHAR(256) NOT NULL;

# Add primary key to PrimaryStorageHostRefVO and make SharedBlockGroupPrimaryStorageHostRefVO inherit it

ALTER TABLE PrimaryStorageHostRefVO ADD id BIGINT UNSIGNED NOT NULL UNIQUE AUTO_INCREMENT;
ALTER TABLE PrimaryStorageHostRefVO DROP FOREIGN KEY fkPrimaryStorageHostRefVOHostEO, DROP FOREIGN KEY fkPrimaryStorageHostRefVOPrimaryStorageEO;
ALTER TABLE PrimaryStorageHostRefVO DROP PRIMARY KEY, ADD PRIMARY KEY ( `id` );
ALTER TABLE PrimaryStorageHostRefVO ADD CONSTRAINT fkPrimaryStorageHostRefVOHostEO FOREIGN KEY (hostUuid) REFERENCES HostEO (uuid) ON DELETE CASCADE;
ALTER TABLE PrimaryStorageHostRefVO ADD CONSTRAINT fkPrimaryStorageHostRefVOPrimaryStorageEO FOREIGN KEY (primaryStorageUuid) REFERENCES PrimaryStorageEO (uuid) ON DELETE CASCADE;
INSERT INTO PrimaryStorageHostRefVO (primaryStorageUuid, hostUuid, status, lastOpDate, createDate) SELECT s.primaryStorageUuid, s.hostUuid, s.status, s.lastOpDate, s.createDate FROM SharedBlockGroupPrimaryStorageHostRefVO s;

ALTER TABLE SharedBlockGroupPrimaryStorageHostRefVO DROP FOREIGN KEY fkSharedBlockGroupPrimaryStorageHostRefVOPrimaryStorageEO, DROP FOREIGN KEY fkSharedBlockGroupPrimaryStorageHostRefVOHostEO;
ALTER TABLE SharedBlockGroupPrimaryStorageHostRefVO DROP INDEX ukSharedBlockGroupPrimaryStorageHostRefVO;
ALTER TABLE SharedBlockGroupPrimaryStorageHostRefVO ADD id BIGINT UNSIGNED UNIQUE;
UPDATE SharedBlockGroupPrimaryStorageHostRefVO s, PrimaryStorageHostRefVO p SET s.id = p.id WHERE s.primaryStorageUuid = p.primaryStorageUuid AND s.hostUuid = p.hostUuid;
ALTER TABLE SharedBlockGroupPrimaryStorageHostRefVO ADD CONSTRAINT fkSharedBlockGroupPrimaryStorageHostRefVOPrimaryStorageHostRefVO FOREIGN KEY (id) REFERENCES PrimaryStorageHostRefVO (id) ON DELETE CASCADE;
ALTER TABLE SharedBlockGroupPrimaryStorageHostRefVO DROP PRIMARY KEY, ADD PRIMARY KEY ( `id` );
ALTER TABLE SharedBlockGroupPrimaryStorageHostRefVO DROP COLUMN primaryStorageUuid, DROP COLUMN hostUuid, DROP COLUMN status, DROP COLUMN lastOpDate, DROP COLUMN createDate;

-- ----------------------------
--  For unattended baremetal provisioning
-- ----------------------------
CREATE TABLE `PreconfigurationTemplateVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `distribution` VARCHAR(64) NOT NULL,
    `type` VARCHAR(32) NOT NULL,
    `content` MEDIUMTEXT NOT NULL,
    `md5sum` VARCHAR(255) NOT NULL,
    `isPredefined` TINYINT(1) UNSIGNED DEFAULT 0,
    `state` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `TemplateCustomParamVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `templateUuid` VARCHAR(32) NOT NULL,
    `param` VARCHAR(255) NOT NULL,
    CONSTRAINT fkTemplateCustomParamVOPreconfigurationTemplateVO FOREIGN KEY (templateUuid) REFERENCES PreconfigurationTemplateVO (uuid) ON DELETE CASCADE,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `CustomPreconfigurationVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `baremetalInstanceUuid` VARCHAR(32) NOT NULL,
    `param` VARCHAR(255) NOT NULL,
    `value` TEXT NOT NULL,
    CONSTRAINT fkCustomPreconfigurationVOBaremetalInstanceVO FOREIGN KEY (baremetalInstanceUuid) REFERENCES BaremetalInstanceVO (uuid) ON DELETE CASCADE,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- we don't know mac address of the bm vlan nic
ALTER TABLE `BaremetalNicVO` DROP INDEX `mac`;
ALTER TABLE `BaremetalNicVO` MODIFY `mac` varchar(17) DEFAULT NULL;
ALTER TABLE `BaremetalNicVO` ADD COLUMN `baremetalBondingUuid` varchar(32) DEFAULT NULL;
ALTER TABLE `BaremetalNicVO` ADD CONSTRAINT `ukBaremetalNicVO` UNIQUE (`mac`,`baremetalBondingUuid`);

CREATE TABLE  `BaremetalVlanNicVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `vlan` int unsigned NOT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
ALTER TABLE BaremetalVlanNicVO ADD CONSTRAINT fkBaremetalVlanNicVOBaremetalNicVO FOREIGN KEY (uuid) REFERENCES BaremetalNicVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;

CREATE TABLE `BaremetalBondingVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `chassisUuid` VARCHAR(32) NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    `mode` TINYINT UNSIGNED NOT NULL,
    `slaves` VARCHAR(2048) NOT NULL,
    `opts` VARCHAR(1024) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `BaremetalInstanceVO` ADD COLUMN `templateUuid` varchar(32) DEFAULT NULL;
ALTER TABLE `BaremetalInstanceVO` ADD CONSTRAINT `fkBaremetalInstanceVOPreconfigurationTemplateVO` FOREIGN KEY (`templateUuid`) REFERENCES `PreconfigurationTemplateVO` (`uuid`) ON DELETE SET NULL;

CREATE TABLE `RouterAreaVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE COMMENT 'area uuid',
    `areaId` VARCHAR(64) NOT NULL COMMENT 'area id 32bit with IPv4 address style',
    `type` VARCHAR(16) NOT NULL DEFAULT 'Standard',
    `authentication` VARCHAR(16) NOT NULL DEFAULT 'None',
    `password` VARCHAR(16) DEFAULT NULL,
    `keyId` int unsigned DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `NetworkRouterAreaRefVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `routerAreaUuid` VARCHAR(32) NOT NULL,
    `vRouterUuid` VARCHAR(32) NOT NULL,
    `l3NetworkUuid` VARCHAR(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkNetworkRouterAreaRefVORouterAreaVO` FOREIGN KEY (`routerAreaUuid`) REFERENCES `RouterAreaVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkNetworkRouterAreaRefVOL3NetworkVO` FOREIGN KEY (`l3NetworkUuid`) REFERENCES `L3NetworkEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkNetworkRouterAreaRefVOVpcRouterVmVO` FOREIGN KEY (`vRouterUuid`) REFERENCES `VpcRouterVmVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX idxVmUuid ON VmUsageVO(vmUuid) USING BTREE;

DELIMITER $$
CREATE PROCEDURE cleanExpireVmUsageVO()
		BEGIN
				DECLARE done INT DEFAULT FALSE;
			  DECLARE vmUuid VARCHAR(32);
				DECLARE name VARCHAR(255);
				DECLARE accountUuid VARCHAR(32);
				DECLARE cpuNum INT(10);
				DECLARE state VARCHAR(64);
				DECLARE memorySize BIGINT(20);
				DECLARE rootVolumeSize BIGINT(20);
			  DECLARE inventory Text;
				DECLARE lastOpDate TIMESTAMP;
				DEClARE cur CURSOR FOR SELECT v.vmUuid,v.name,v.accountUuid,v.state,v.cpuNum,v.memorySize,v.rootVolumeSize,v.inventory from VmUsageVO v
								where v.id IN (select MAX(a.id) FROM VmUsageVO a GROUP BY a.vmUuid)
								AND v.vmUuid NOT IN (select DISTINCT uuid from VmInstanceEO) AND v.state = 'Running';
				DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
			  OPEN cur;
				read_loop: LOOP
						FETCH cur INTO vmUuid,name,accountUuid,state,cpuNum,memorySize,rootVolumeSize,inventory;
						IF done THEN
								LEAVE read_loop;
						END IF;

						INSERT zstack.VmUsageVO(vmUuid,name,accountUuid,state,cpuNum,memorySize,dateInLong,rootVolumeSize,inventory,lastOpDate,createDate)
						VALUES (vmUuid,name,accountUuid,'Destroyed',cpuNum,memorySize,UNIX_TIMESTAMP(),rootVolumeSize,inventory,NOW(),NOW());

				END LOOP;
				CLOSE cur;
				SELECT CURTIME();
		END $$
DELIMITER ;

call cleanExpireVmUsageVO();
DROP PROCEDURE IF EXISTS cleanExpireVmUsageVO;

CREATE TABLE `ResourceConfigVO` (
    `uuid`         VARCHAR(32)  NOT NULL UNIQUE,
    `name`         VARCHAR(255) NOT NULL,
    `description`  VARCHAR(1024) DEFAULT NULL,
    `category`     VARCHAR(64)  NOT NULL,
    `value`        TEXT         NOT NULL,
    `resourceUuid` VARCHAR(32)  NOT NULL,
    `resourceType` VARCHAR(256) NOT NULL,
    `lastOpDate`   TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate`   TIMESTAMP,
    PRIMARY KEY (`uuid`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;
ALTER TABLE ResourceConfigVO ADD CONSTRAINT fkResourceConfigVOResourceVO FOREIGN KEY (`resourceUuid`) REFERENCES `ResourceVO` (uuid) ON DELETE CASCADE;

DELIMITER $$
CREATE PROCEDURE migrateReserveMemTagVO()
    BEGIN
        DECLARE done INT DEFAULT FALSE;
        DECLARE tag VARCHAR(64);
        DECLARE resourceUuid VARCHAR(32);
        DECLARE resourceType VARCHAR(32);
        DECLARE resourceConfigUuid VARCHAR(32);
        DECLARE des VARCHAR(1024);
        DECLARE cur1 CURSOR FOR SELECT DISTINCT stag.tag, stag.resourceUuid, stag.resourceType FROM zstack.SystemTagVO stag WHERE stag.tag LIKE 'reservedMemory::%';
        DECLARE cur2 CURSOR FOR SELECT DISTINCT stag.tag, stag.resourceUuid, stag.resourceType FROM zstack.SystemTagVO stag WHERE stag.tag LIKE 'host::reservedMemory::%';
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

        SET des = 'The memory capacity reserved on all KVM hosts. ZStack KVM agent is a python web server that needs some memory capacity to run. this value reserves a portion of memory for the agent as well as other host applications. The value can be overridden by system tag on individual host, cluster and zone level';

        OPEN cur1;
        read_loop: LOOP
            FETCH cur1 INTO tag, resourceUuid, resourceType;
            IF done THEN
                LEAVE read_loop;
            END IF;

            SET resourceConfigUuid = (REPLACE(UUID(), '-', ''));
            INSERT ResourceConfigVO(uuid, name, description, category, value, resourceUuid, resourceType, createDate, lastOpDate)
            VALUES (resourceConfigUuid, 'reservedMemory', des, 'kvm', substring(tag, LENGTH('reservedMemory::') + 1), resourceUuid ,resourceType, NOW(), NOW());

        END LOOP;
        CLOSE cur1;

        SET done = FALSE;
        OPEN cur2;
        read_loop: LOOP
            FETCH cur2 INTO tag, resourceUuid, resourceType;
            IF done THEN
                LEAVE read_loop;
            END IF;

            SET resourceConfigUuid = (REPLACE(UUID(), '-', ''));
            INSERT ResourceConfigVO(uuid, name, description, category, value, resourceUuid, resourceType, createDate, lastOpDate)
            VALUES (resourceConfigUuid, 'reservedMemory', des, 'kvm', substring(tag, LENGTH('host::reservedMemory::') + 1), resourceUuid ,resourceType, NOW(), NOW());

        END LOOP;
        CLOSE cur2;
        SELECT CURTIME();
    END $$
DELIMITER ;

call migrateReserveMemTagVO();
DROP PROCEDURE IF EXISTS migrateReserveMemTagVO;

ALTER TABLE VpcVpnGatewayVO CHANGE endDate endDate datetime NOT NULL;

CREATE TABLE `ResourceUsageVO` (
  `id` BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `accountUuid`  VARCHAR(32) NOT NULL,
  `resourceType` VARCHAR(32) NOT NULL,
  `resourceUuid` VARCHAR(32) NOT NULL,
  `resourceName` VARCHAR(255) NOT NULL,
  `spending`     DOUBLE NOT NULL,
  `spendingDate`  DATE NOT NULL,
  `spendingStart` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00',
  `spendingEnd`   TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00',
  `createDate` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idxResourceUsageVOaccountUuid` (`accountUuid`),
  INDEX `idxResourceUsageVOspendingDate` (`spendingDate`),
  INDEX `idxResourceUsageVOresourceUuid` (`resourceUuid`),
  INDEX `idxResourceUsageVOtypeDate` (`resourceType`,`spendingDate`),
  UNIQUE `idxResourceUsageVOuuidDate` (`resourceType`,`resourceUuid`,`spendingDate`)
) ENGINE=INNODB DEFAULT CHARSET=UTF8;

INSERT INTO AccountResourceRefVO (`accountUuid`, `ownerAccountUuid`, `resourceUuid`, `resourceType`, `permission`, `isShared`, `lastOpDate`, `createDate`, `concreteResourceType`) SELECT "36c27e8ff05c4780bf6d2fa65700f22e", "36c27e8ff05c4780bf6d2fa65700f22e", t.uuid, "L2NetworkVO", 2, 0, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), "org.zstack.header.network.l2.L2VlanNetworkVO" FROM L2NetworkVO t where t.type  = "L2VlanNetwork" AND t.uuid NOT IN (SELECT resourceUuid FROM AccountResourceRefVO);
INSERT INTO AccountResourceRefVO (`accountUuid`, `ownerAccountUuid`, `resourceUuid`, `resourceType`, `permission`, `isShared`, `lastOpDate`, `createDate`, `concreteResourceType`) SELECT "36c27e8ff05c4780bf6d2fa65700f22e", "36c27e8ff05c4780bf6d2fa65700f22e", t.uuid, "L2NetworkVO", 2, 0, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), "org.zstack.header.network.l2.L2NetworkVO" FROM L2NetworkVO t where t.type  = "L2NoVlanNetwork" AND t.uuid NOT IN (SELECT resourceUuid FROM AccountResourceRefVO);

ALTER TABLE `AlarmVO` ADD COLUMN `enableRecovery` boolean NOT NULL DEFAULT FALSE;
ALTER TABLE `SNSTextTemplateVO` ADD COLUMN  `recoveryTemplate` text DEFAULT NULL;

DROP PROCEDURE IF EXISTS initializeRecoveryTemplate;
DELIMITER $$
CREATE PROCEDURE initializeRecoveryTemplate()
		BEGIN
				DECLARE done INT DEFAULT FALSE;
			  DECLARE uuid VARCHAR(32);
			  DECLARE applicationPlatformType VARCHAR(128);
				DECLARE cur CURSOR FOR SELECT v.uuid,v.applicationPlatformType FROM SNSTextTemplateVO v WHERE v.recoveryTemplate IS NULL or v.recoveryTemplate = '';
				DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
			  OPEN cur;
				read_loop: LOOP
						FETCH cur INTO uuid,applicationPlatformType;
						IF done THEN
								LEAVE read_loop;
						END IF;

						IF applicationPlatformType = 'DingTalk' THEN
						    UPDATE SNSTextTemplateVO v SET v.recoveryTemplate = replace('# 报警器 %{ALARM_NAME} %{TITLE_ALARM_RESOURCE_NAME} 状态改变成 %{ALARM_CURRENT_STATUS}\n## 报警恢复详情:\n- UUID: %{ALARM_UUID}\n- 资源名字空间: %{ALARM_NAMESPACE}\n- 恢复条件: %{ALARM_METRIC} %{ALARM_COMPARISON_OPERATOR_REVERSE} %{ALARM_THRESHOLD}\n- 先前状态: %{ALARM_PREVIOUS_STATUS}\n- 当前值: %{ALARM_CURRENT_VALUE}\n- 报警资源UUID: %{ALARM_RESOURCE_ID}\n- 报警资源名称: %{ALARM_RESOURCE_NAME}', '%', '$') WHERE v.uuid = uuid;
            END IF;

						IF applicationPlatformType = 'Email' THEN
						    UPDATE SNSTextTemplateVO v SET v.recoveryTemplate = replace('报警器 %{ALARM_NAME} %{TITLE_ALARM_RESOURCE_NAME} 状态改变成 %{ALARM_CURRENT_STATUS}\n报警恢复详情:\nUUID: %{ALARM_UUID}\n资源名字空间: %{ALARM_NAMESPACE}\n恢复条件: %{ALARM_METRIC} %{ALARM_COMPARISON_OPERATOR_REVERSE} %{ALARM_THRESHOLD}\n先前状态: %{ALARM_PREVIOUS_STATUS}\n当前值: %{ALARM_CURRENT_VALUE}\n报警资源UUID: %{ALARM_RESOURCE_ID}\n报警资源名称: %{ALARM_RESOURCE_NAME}' , '%', '$') WHERE v.uuid = uuid;
            END IF;
				END LOOP;
				CLOSE cur;
				SELECT CURTIME();
		END $$
DELIMITER ;

call initializeRecoveryTemplate();
DROP PROCEDURE IF EXISTS initializeRecoveryTemplate;

ALTER TABLE `SchedulerTriggerVO` MODIFY COLUMN `cron` varchar(255) DEFAULT NULL COMMENT 'interval in cron format';

ALTER TABLE `HybridAccountVO` MODIFY `name` VARCHAR(255) UNIQUE NOT NULL;

CREATE TABLE `zstack`.`SchedulerJobGroupVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `jobType` VARCHAR(32),
    `jobClassName` varchar(255),
    `jobData` text,
    `state` varchar(255),
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `zstack`.`SchedulerJobGroupJobRefVO` (
    `schedulerJobUuid` varchar(32) NOT NULL,
    `schedulerJobGroupUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY (`schedulerJobUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `zstack`.`SchedulerJobGroupSchedulerTriggerRefVO` (
    `schedulerJobGroupUuid` varchar(32) NOT NULL,
    `schedulerTriggerUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`schedulerJobGroupUuid`, `schedulerTriggerUuid`),
    CONSTRAINT `fkSchedulerJobGroupSchedulerTriggerRefVOSchedulerJobGroupVO` FOREIGN KEY (`schedulerJobGroupUuid`) REFERENCES `SchedulerJobGroupVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkSchedulerJobGroupSchedulerTriggerRefVOSchedulerTriggerVO` FOREIGN KEY (`schedulerTriggerUuid`) REFERENCES `SchedulerTriggerVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DELIMITER $$
CREATE PROCEDURE migrateSchedulerJob()
    BEGIN
        DECLARE done INT DEFAULT FALSE;
        DECLARE triggerUuid VARCHAR(32);
        DECLARE groupUuid VARCHAR(32);
        DECLARE volumeType VARCHAR(32);
        DECLARE volumeUuid VARCHAR(32);
        DECLARE legacyJobUuid VARCHAR(32);
        DECLARE legacyJobName VARCHAR(255);
        DECLARE legacyJobDescription VARCHAR(2048);
        DECLARE legacyJobClassName VARCHAR(255);
        DECLARE legacyJobData TEXT;
        DECLARE legacyJobstate VARCHAR(255);
        DECLARE legacyJobAccountUuid VARCHAR(32);
        DECLARE groupJobType VARCHAR(32);
        DEClARE cur CURSOR FOR SELECT uuid, name, description, jobClassName, jobData, state, targetResourceUuid from SchedulerJobVO
        where jobClassName in ('org.zstack.storage.backup.CreateVolumeBackupJob', 'org.zstack.storage.backup.CreateVmBackupJob');
        DEClARE tcur CURSOR FOR SELECT schedulerTriggerUuid from SchedulerJobSchedulerTriggerRefVO where schedulerJobUuid = legacyJobUuid;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
        OPEN cur;
        insert_group_loop: LOOP
            FETCH cur INTO legacyJobUuid, legacyJobName, legacyJobDescription, legacyJobClassName, legacyJobData, legacyJobstate, volumeUuid;
            IF done THEN
                LEAVE insert_group_loop;
            END IF;

            IF legacyJobClassName = 'org.zstack.storage.backup.CreateVolumeBackupJob' THEN
                SELECT `type` INTO volumeType FROM `VolumeVO` WHERE `uuid` = volumeUuid;
                IF volumeType = 'Root' THEN
                    SET groupJobType = 'rootVolumeBackup';
                ELSE
                    SET groupJobType = 'volumeBackup';
                END IF;
            ELSE
                SET groupJobType = 'vmBackup';
            END IF;

            SELECT DISTINCT accountUuid INTO legacyJobAccountUuid FROM AccountResourceRefVO WHERE resourceUuid = legacyJobUuid;
            SET groupUuid = (REPLACE(UUID(), '-', ''));
            INSERT INTO zstack.SchedulerJobGroupVO(uuid, name, description, jobClassName, jobData, jobType, state, lastOpDate, createDate)
            VALUES(groupUuid, legacyJobName, legacyJobDescription, legacyJobClassName, legacyJobData, groupJobType, legacyJobstate, NOW(), NOW());
            INSERT INTO zstack.ResourceVO(uuid, resourceName, resourceType, concreteResourceType)
            VALUES(groupUuid, legacyJobName, 'SchedulerJobGroupVO', 'org.zstack.header.scheduler.SchedulerJobGroupVO');
            INSERT INTO zstack.AccountResourceRefVO (accountUuid, ownerAccountUuid, resourceUuid, resourceType, permission, isShared, lastOpDate, createDate, concreteResourceType)
            VALUES(legacyJobAccountUuid, legacyJobAccountUuid, groupUuid, 'SchedulerJobGroupVO', 2, 0,  NOW(), NOW(), 'org.zstack.header.scheduler.SchedulerJobGroupVO');

            INSERT INTO zstack.SchedulerJobGroupJobRefVO(schedulerJobUuid, schedulerJobGroupUuid, lastOpDate, createDate)
            VALUES(legacyJobUuid, groupUuid, NOW(), NOW());

            OPEN tcur;
            migrate_ref_loop: LOOP
                FETCH tcur INTO triggerUuid;
                IF done THEN
                    LEAVE migrate_ref_loop;
                END IF;

                INSERT INTO zstack.SchedulerJobGroupSchedulerTriggerRefVO(schedulerJobGroupUuid, schedulerTriggerUuid, lastOpDate, createDate)
                VALUES (groupUuid, triggerUuid, NOW(), NOW());

            END LOOP;
            CLOSE tcur;

            DELETE FROM zstack.SchedulerJobSchedulerTriggerRefVO where schedulerJobUuid = legacyJobUuid;
            SET done = FALSE;
        END LOOP;
        CLOSE cur;
    END $$
DELIMITER ;

call migrateSchedulerJob();
DROP PROCEDURE IF EXISTS migrateSchedulerJob;

ALTER TABLE `SchedulerTriggerVO` MODIFY COLUMN `cron` varchar(255) DEFAULT NULL COMMENT 'interval in cron format';
ALTER TABLE `VolumeBackupVO` DROP FOREIGN KEY `fkVolumeBackupVOResourceVO`;
ALTER TABLE `DatabaseBackupVO` DROP FOREIGN KEY `fkDatabaseBackupVOResourceVO`;
