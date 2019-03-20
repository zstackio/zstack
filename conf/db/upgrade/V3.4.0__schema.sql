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
DELIMITER;

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

INSERT INTO AccountResourceRefVO (`accountUuid`, `ownerAccountUuid`, `resourceUuid`, `resourceType`, `permission`, `isShared`, `lastOpDate`, `createDate`) SELECT "36c27e8ff05c4780bf6d2fa65700f22e", "36c27e8ff05c4780bf6d2fa65700f22e", t.uuid, "L2NetworkVO", 2, 0, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP() FROM L2NetworkVO t where t.type in ("L2VlanNetwork", "L2NoVlanNetwork");
