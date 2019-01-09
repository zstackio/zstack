CREATE TABLE IF NOT EXISTS `ElaborationVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `errorInfo` text NOT NULL,
  `md5sum` varchar(32) NOT NULL,
  `distance` double NOT NULL,
  `matched` boolean NOT NULL DEFAULT FALSE,
  `repeats` bigint(20) unsigned NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX idxElaborationVOmd5sum ON ElaborationVO (md5sum);

CREATE TABLE  `zstack`.`VCenterResourcePoolVO` (
    `uuid` varchar(32) NOT NULL COMMENT 'VCenter Resource Pool uuid',
    `vCenterClusterUuid` varchar(32) NOT NULL COMMENT 'VCenter cluster uuid',
    `name` varchar(256) NOT NULL COMMENT 'VCenter Resource Pool name',
    `morVal` varchar(256) NOT NULL COMMENT 'VCenter Resource Pool management object value in vcenter',
    `parentUuid` varchar(32) COMMENT 'Parent Resource Pool uuid or NULL',
    `CPULimit` bigint(64),
    `CPUOverheadLimit` bigint(64),
    `CPUReservation` bigint(64),
    `CPUShares` bigint(64),
    `CPULevel` varchar(64),
    `MemoryLimit` bigint(64),
    `MemoryOverheadLimit` bigint(64),
    `MemoryReservation` bigint(64),
    `MemoryShares` bigint(64),
    `MemoryLevel` varchar(64),
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkVCenterResourcePoolVOVCenterClusterVO` FOREIGN KEY (`vCenterClusterUuid`) REFERENCES `VCenterClusterVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`VCenterResourcePoolUsageVO` (
    `uuid` varchar(32) NOT NULL COMMENT 'VCenter Resource Pool usage uuid',
    `vCenterResourcePoolUuid` varchar(32) NOT NULL COMMENT 'VCenter Resource Pool uuid',
    `resourceUuid` varchar(32) NOT NULL COMMENT 'VCenter Resource resource uuid',
    `resourceType` varchar(256) NOT NULL COMMENT 'VCenter Resource resource type',
    `resourceName` varchar(256) COMMENT 'VCenter Resource resource name',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
     PRIMARY KEY  (`uuid`),
     UNIQUE KEY `VCenterResourcePoolUsageVO` (`vCenterResourcePoolUuid`, `resourceUuid`) USING BTREE,
     CONSTRAINT `fkVCenterResourcePoolUsageVOVCenterResourcePoolVO` FOREIGN KEY (`vCenterResourcePoolUuid`) REFERENCES `VCenterResourcePoolVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# create missing tag2 role for IAM2ProjectVO
DELIMITER $$
CREATE PROCEDURE getRoleUuid(OUT targetRoleUuid VARCHAR(32))
    BEGIN
        SELECT uuid into targetRoleUuid from RoleVO role where role.name = 'predefined: tag2' and role.type = 'Predefined' LIMIT 0,1;
    END $$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE getRolePolicyStatement(OUT policyStatement text, IN targetRoleUuid VARCHAR(32))
    BEGIN
        SELECT statement into policyStatement from RolePolicyStatementVO where roleUuid = targetRoleUuid LIMIT 0,1;
    END $$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE getMaxAccountResourceRefVO(OUT refId bigint(20) unsigned)
    BEGIN
        SELECT max(id) INTO refId from zstack.AccountResourceRefVO;
    END $$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE fixMissingTag2RoleInProjects()
    BEGIN
        DECLARE done INT DEFAULT FALSE;
        DECLARE count_tag_role INT DEFAULT 0;
        DECLARE count_tag_role_for_project INT DEFAULT 0;
        DECLARE targetAccountUuid varchar(32);
        DECLARE targetRoleUuid varchar(32);
        DECLARE new_role_uuid VARCHAR(32);
        DECLARE new_statement_uuid VARCHAR(32);
        DECLARE refId bigint(20) unsigned;
        DECLARE policyStatement text;
        DECLARE cur CURSOR FOR SELECT accountUuid FROM zstack.IAM2ProjectAccountRefVO;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

        OPEN cur;
        CALL getRoleUuid(targetRoleUuid);

        read_loop: LOOP
            FETCH cur INTO targetAccountUuid;
            IF done THEN
                LEAVE read_loop;
            END IF;

            SELECT count(*) into count_tag_role from RoleVO role where role.name = 'predefined: tag2' and role.type = 'Predefined';
            IF (count_tag_role != 0) THEN
               SELECT count(*) into count_tag_role_for_project from RoleVO role, AccountResourceRefVO ref
               where role.name = 'predefined: tag2' and role.type = 'CreatedBySystem'
               and ref.resourceUuid = role.uuid and ref.accountUuid = targetAccountUuid;

               IF (count_tag_role_for_project < 1) THEN
                   SET new_role_uuid = REPLACE(UUID(), '-', '');

                   INSERT INTO ResourceVO (`uuid`, `resourceName`, `resourceType`, `concreteResourceType`)
                   values (new_role_uuid, 'predefined: tag2', 'RoleVO', 'org.zstack.header.identity.role.RoleVO');

                   INSERT INTO RoleVO (`uuid`, `name`, `type`, `state`, `description`, `lastOpDate`, `createDate`)
                   values (new_role_uuid, 'predefined: tag2', 'CreatedBySystem', 'Enabled', NULL, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

                   CALL getMaxAccountResourceRefVO(refId);
                   INSERT INTO AccountResourceRefVO (`id`, `accountUuid`, `ownerAccountUuid`, `resourceUuid`, `resourceType`, `permission`, `isShared`, `lastOpDate`, `createDate`)
                   values (refId + 1, targetAccountUuid, targetAccountUuid, new_role_uuid, 'RoleVO', 2, 0, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

                   SET new_statement_uuid = REPLACE(UUID(), '-', '');
                   CALL getRandomUuid(new_statement_uuid);
                   CALL getRolePolicyStatement(policyStatement, targetRoleUuid);
                   INSERT INTO RolePolicyStatementVO (`uuid`, `statement`, `roleUuid`, `lastOpDate`, `createDate`)
                   values (new_statement_uuid, policyStatement, new_role_uuid, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
               END IF;
            END IF;
        END LOOP;
        CLOSE cur;
        SELECT CURTIME();
    END $$
DELIMITER ;

call fixMissingTag2RoleInProjects();
DROP PROCEDURE IF EXISTS fixMissingTag2RoleInProjects;
DROP PROCEDURE IF EXISTS getMaxAccountResourceRefVO;
DROP PROCEDURE IF EXISTS getRolePolicyStatement;
DROP PROCEDURE IF EXISTS getRoleUuid;
