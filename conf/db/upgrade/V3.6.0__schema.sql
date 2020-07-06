INSERT INTO AccountResourceRefVO (`accountUuid`, `ownerAccountUuid`, `resourceUuid`, `resourceType`, `permission`, `isShared`, `lastOpDate`, `createDate`, `concreteResourceType`) SELECT "36c27e8ff05c4780bf6d2fa65700f22e", "36c27e8ff05c4780bf6d2fa65700f22e", t.uuid, "VCenterVO", 2, 0, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), "org.zstack.vmware.VCenterVO" FROM VCenterVO t where t.uuid NOT IN (SELECT resourceUuid FROM AccountResourceRefVO);

CREATE TABLE `zstack`.`VolumeSnapshotGroupVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `vmInstanceUuid` VARCHAR(32) NOT NULL,
    `snapshotCount` int unsigned NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `zstack`.`VolumeSnapshotGroupRefVO` (
    `volumeSnapshotUuid` VARCHAR(32) NOT NULL UNIQUE,
    `volumeSnapshotGroupUuid` VARCHAR(32) NOT NULL,
    `snapshotDeleted` BOOLEAN NOT NULL,
    `deviceId` int unsigned NOT NULL,
    `volumeUuid` VARCHAR(32) NOT NULL,
    `volumeName` VARCHAR(256) NOT NULL,
    `volumeType` VARCHAR(32) NOT NULL,
    `volumeSnapshotName` varchar(256) DEFAULT NULL,
    `volumeSnapshotInstallPath` varchar(1024) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY (`volumeSnapshotUuid`),
    CONSTRAINT `fkVolumeSnapshotGroupRefVOVolumeSnapshotGroupVO` FOREIGN KEY (`volumeSnapshotGroupUuid`) REFERENCES `VolumeSnapshotGroupVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP PROCEDURE IF EXISTS getMaxAccountResourceRefVO;
DROP PROCEDURE IF EXISTS upgradePrivilegeAdmin;
DROP PROCEDURE IF EXISTS createRoleRefsInProject;
DROP PROCEDURE IF EXISTS upgradeIAM2ReadRole;

DELIMITER $$
CREATE PROCEDURE getMaxAccountResourceRefVO(OUT refId bigint(20) unsigned)
    BEGIN
        SELECT max(id) INTO refId from zstack.AccountResourceRefVO;
    END $$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE upgradePrivilegeAdmin(IN privilege_role_uuid VARCHAR(32), IN role_name VARCHAR(255))
    procedure_label: BEGIN
        DECLARE role_count INT DEFAULT 0;
        DECLARE done INT DEFAULT FALSE;
        DECLARE vid varchar(32);
        DECLARE role_statement_uuid varchar(32);
        DECLARE new_statement_uuid varchar(32);
        DECLARE refId bigint(20) unsigned;
        DECLARE generated_role_uuid VARCHAR(32);
        DECLARE cur CURSOR FOR SELECT virtualIDUuid FROM zstack.IAM2VirtualIDRoleRefVO WHERE roleUuid=privilege_role_uuid;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
        SELECT count(*) INTO role_count FROM zstack.RoleVO WHERE uuid = privilege_role_uuid;

        IF (role_count = 0) THEN
            SELECT CURTIME();
            LEAVE procedure_label;
        END IF;

        SELECT uuid INTO role_statement_uuid FROM RolePolicyStatementVO WHERE roleUuid = privilege_role_uuid LIMIT 1;

        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO vid;
            IF done THEN
                LEAVE read_loop;
            END IF;

            SET generated_role_uuid = REPLACE(UUID(), '-', '');

            INSERT INTO ResourceVO (`uuid`, `resourceName`, `resourceType`, `concreteResourceType`)
            VALUES (generated_role_uuid, role_name, 'RoleVO', 'org.zstack.header.identity.role.RoleVO');

            INSERT INTO zstack.RoleVO (`uuid`, `name`, `createDate`, `lastOpDate`, `state`, `type`)
            SELECT generated_role_uuid, role_name, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), `state`, 'Customized' FROM
            RoleVO WHERE uuid = privilege_role_uuid;

            CALL getMaxAccountResourceRefVO(refId);
            INSERT INTO AccountResourceRefVO (`id`, `accountUuid`, `ownerAccountUuid`, `resourceUuid`, `resourceType`, `permission`, `isShared`, `lastOpDate`, `createDate`)
            VALUES (refId + 1, '36c27e8ff05c4780bf6d2fa65700f22e', '36c27e8ff05c4780bf6d2fa65700f22e', generated_role_uuid, 'RoleVO', 2, 0, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

            SET new_statement_uuid = REPLACE(UUID(), '-', '');
            INSERT INTO zstack.RolePolicyStatementVO (`uuid`, `statement`, `roleUuid`, `lastOpDate`, `createDate`)
            SELECT new_statement_uuid, `statement`, generated_role_uuid, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP() FROM
            RolePolicyStatementVO WHERE uuid = role_statement_uuid;

            INSERT INTO IAM2VirtualIDRoleRefVO (`virtualIDUuid`, `roleUuid`, `lastOpDate`, `createDate`)
            VALUES (vid, generated_role_uuid, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
        END LOOP;
        CLOSE cur;

        DELETE FROM zstack.IAM2VirtualIDRoleRefVO WHERE roleUuid = privilege_role_uuid;
        DELETE FROM zstack.RolePolicyStatementVO WHERE roleUuid = privilege_role_uuid;
        DELETE FROM zstack.RoleVO WHERE uuid = privilege_role_uuid;
        DELETE FROM zstack.ResourceVO WHERE uuid = privilege_role_uuid;
        DELETE FROM zstack.AccountResourceRefVO WHERE resourceUuid = privilege_role_uuid;
        SELECT CURTIME();
    END $$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE createRoleRefsInProject(IN project_uuid VARCHAR(32), IN role_uuid VARCHAR(32))
    BEGIN
        DECLARE vid varchar(32);
        DECLARE done INT DEFAULT FALSE;
        DECLARE cur CURSOR FOR SELECT virtualIDUuid FROM zstack.IAM2ProjectVirtualIDRefVO where projectUuid = project_uuid;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO vid;
            IF done THEN
                LEAVE read_loop;
            END IF;

            INSERT INTO IAM2VirtualIDRoleRefVO (`virtualIDUuid`, `roleUuid`, `lastOpDate`, `createDate`)
            VALUES (vid, role_uuid, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
        END LOOP;
        CLOSE cur;
        SELECT CURTIME();
    END $$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE upgradeIAM2ReadRole(IN role_name VARCHAR(255))
    upgrade_label: BEGIN
        DECLARE done INT DEFAULT FALSE;
        DECLARE only_update_vid_statement varchar(255);
        DECLARE read_role_uuid varchar(32);
        DECLARE generated_role_uuid varchar(32);
        DECLARE new_statement_uuid varchar(32);
        DECLARE project_uuid varchar(32);
        DECLARE account_uuid varchar(32);
        DECLARE refId bigint(20) unsigned;
        DECLARE cur CURSOR FOR SELECT projectUuid, accountUuid FROM zstack.IAM2ProjectAccountRefVO;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

        SET only_update_vid_statement = '{"name":"default-apis-for-normal-virtualID","effect":"Allow","actions":["org.zstack.iam2.api.APIUpdateIAM2VirtualIDMsg"]}';

        SELECT uuid INTO read_role_uuid FROM zstack.RoleVO WHERE name like "read-api-role-%" and type = 'System'
         and uuid in (SELECT roleUuid from RolePolicyStatementVO statement WHERE CHAR_LENGTH(statement) > CHAR_LENGTH(only_update_vid_statement)) LIMIT 1;

        IF (read_role_uuid = NULL) THEN
            SELECT CURTIME();
            LEAVE upgrade_label;
        END IF;

        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO project_uuid, account_uuid;
            IF done THEN
                LEAVE read_loop;
            END IF;

            SET generated_role_uuid = REPLACE(UUID(), '-', '');

            INSERT INTO ResourceVO (`uuid`, `resourceName`, `resourceType`, `concreteResourceType`)
            VALUES (generated_role_uuid, role_name, 'RoleVO', 'org.zstack.header.identity.role.RoleVO');

            INSERT INTO zstack.RoleVO (`uuid`, `name`, `createDate`, `lastOpDate`, `state`, `type`)
            SELECT generated_role_uuid, role_name, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), `state`, 'Customized' FROM
            RoleVO WHERE uuid = read_role_uuid;

            CALL getMaxAccountResourceRefVO(refId);
            INSERT INTO AccountResourceRefVO (`id`, `accountUuid`, `ownerAccountUuid`, `resourceUuid`, `resourceType`, `permission`, `isShared`, `lastOpDate`, `createDate`)
            VALUES (refId + 1, account_uuid, account_uuid, generated_role_uuid, 'RoleVO', 2, 0, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

            SET new_statement_uuid = REPLACE(UUID(), '-', '');
            INSERT INTO zstack.RolePolicyStatementVO (`uuid`, `statement`, `roleUuid`, `lastOpDate`, `createDate`)
            SELECT new_statement_uuid, `statement`, generated_role_uuid, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP() FROM
            RolePolicyStatementVO WHERE roleUuid = read_role_uuid;

            CALL createRoleRefsInProject(project_uuid, generated_role_uuid);
        END LOOP;
        CLOSE cur;

        UPDATE zstack.RolePolicyStatementVO SET statement = only_update_vid_statement WHERE roleUuid IN (SELECT uuid FROM zstack.RoleVO WHERE name like "read-api-role-%" and type = 'System');
        SELECT CURTIME();
    END $$
DELIMITER ;

CALL upgradePrivilegeAdmin('434a5e418a114714848bb0923acfbb9c', 'audit-admin-role');
CALL upgradePrivilegeAdmin('58db081b0bbf4e93b63dc4ac90a423ad', 'security-admin-role');
CALL upgradeIAM2ReadRole('system-read-role');

DROP PROCEDURE IF EXISTS getMaxAccountResourceRefVO;
DROP PROCEDURE IF EXISTS upgradePrivilegeAdmin;
DROP PROCEDURE IF EXISTS createRoleRefsInProject;
DROP PROCEDURE IF EXISTS upgradeIAM2ReadRole;

# delete dirty project admin attributes in db
delete from IAM2VirtualIDAttributeVO where name = '__ProjectAdmin__' and value not in (select uuid from IAM2ProjectVO);

CREATE TABLE  `zstack`.`GlobalConfigTemplateVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `type` varchar(32) NOT NULL,
    `description` varchar(1024) DEFAULT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`TemplateConfigVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `name` varchar(255) NOT NULL,
    `category` varchar(64) NOT NULL,
    `templateUuid` varchar(32) NOT NULL,
    `defaultValue` text DEFAULT NULL,
    `value` text DEFAULT NULL,
    PRIMARY KEY  (`id`),
    CONSTRAINT `GlobalConfigTemplateVOTemplateConfigVO` FOREIGN KEY (`templateUuid`) REFERENCES `GlobalConfigTemplateVO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `FlowMeterVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE COMMENT 'flow meter uuid' ,
    `name` VARCHAR(32) DEFAULT "" ,
    `description` VARCHAR(128) DEFAULT "" ,
    `version` VARCHAR(16) DEFAULT 'V5',
    `type` VARCHAR(16) DEFAULT 'NetFlow',
    `sample` int unsigned DEFAULT 1,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `FlowCollectorVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE COMMENT 'flow collector uuid' ,
    `flowMeterUuid` VARCHAR(32) NOT NULL,
    `name` VARCHAR(32) DEFAULT "" ,
    `description` VARCHAR(128) DEFAULT "" ,
    `server` VARCHAR(64) NOT NULL,
    `port` VARCHAR(16) DEFAULT '2055',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkFlowCollectorVOFlowMeterVO` FOREIGN KEY (`flowMeterUuid`) REFERENCES `FlowMeterVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `FlowRouterVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE COMMENT 'logic flow router uuid for vrouterHA' ,
    `systemID` int unsigned DEFAULT 0,
    `type` VARCHAR(16) NOT NULL DEFAULT 'normal' COMMENT 'router ha type' ,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `NetworkRouterFlowMeterRefVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `flowMeterUuid` VARCHAR(32) NOT NULL,
    `vFlowRouterUuid` VARCHAR(32) NOT NULL,
    `l3NetworkUuid` VARCHAR(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkNetworkRouterFlowMeterRefVOFlowMeterVO` FOREIGN KEY (`flowMeterUuid`) REFERENCES `FlowMeterVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkNetworkRouterFlowMeterRefVOL3NetworkVO` FOREIGN KEY (`l3NetworkUuid`) REFERENCES `L3NetworkEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkNetworkRouterFlowMeterRefVOFlowRouterVmVO` FOREIGN KEY (`vFlowRouterUuid`) REFERENCES `FlowRouterVO` (`uuid`) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PolicyRouteRuleSetVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(16) NOT NULL,
  `vyosName` varchar(32) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PolicyRouteTableVO` (
  `uuid` varchar(255) NOT NULL,
  `tableNumber` int(3) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PolicyRouteRuleVO` (
  `uuid` varchar(32) NOT NULL,
  `ruleNumber` int(4) NOT NULL,
  `ruleSetUuid` varchar(32) NOT NULL,
  `protocol` varchar(32) DEFAULT NULL,
  `tableUuid` varchar(32) DEFAULT NULL,
  `destIp` varchar(255) DEFAULT NULL,
  `sourceIp` varchar(255) DEFAULT NULL,
  `destPort` varchar(255) DEFAULT NULL,
  `sourcePort` varchar(255) DEFAULT NULL,
  `state` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`) USING BTREE,
  KEY `fkPolicyRouteRuleVOPolicyRouteRuleSetVO` (`ruleSetUuid`),
  KEY `fkPolicyRouteRuleVOPolicyRouteTableVO` (`tableUuid`),
  CONSTRAINT `fkPolicyRouteRuleVOPolicyRouteRuleSetVO` FOREIGN KEY (`ruleSetUuid`) REFERENCES `PolicyRouteRuleSetVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkPolicyRouteRuleVOPolicyRouteTableVO` FOREIGN KEY (`tableUuid`) REFERENCES `PolicyRouteTableVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PolicyRouteTableRouteEntryVO` (
  `uuid` varchar(32) NOT NULL,
  `tableUuid` varchar(32) NOT NULL,
  `distance` int(10) DEFAULT NULL,
  `destinationCidr` varchar(64) NOT NULL,
  `nextHopIp` varchar(255) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`) USING BTREE,
  KEY `fkPolicyRouteTableRouteEntryVOPolicyRouteTableVO` (`tableUuid`),
  CONSTRAINT `fkPolicyRouteTableRouteEntryVOPolicyRouteTableVO` FOREIGN KEY (`tableUuid`) REFERENCES `PolicyRouteTableVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PolicyRouteTableVRouterRefVO` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `tableUuid` varchar(32) NOT NULL,
  `vRouterUuid` varchar(32) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`) USING BTREE,
  KEY `fkPolicyRouteTableVRouterRefVcPolicyRouteTableVO` (`tableUuid`),
  KEY `fkPolicyRouteTableVRouterRefVOVirtualRouterVMVO` (`vRouterUuid`),
  CONSTRAINT `fkPolicyRouteTableVRouterRefVOVirtualRouterVMVO` FOREIGN KEY (`vRouterUuid`) REFERENCES `VirtualRouterVmVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkPolicyRouteTableVRouterRefVcPolicyRouteTableVO` FOREIGN KEY (`tableUuid`) REFERENCES `PolicyRouteTableVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PolicyRouteRuleSetVRouterRefVO` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `vRouterUuid` varchar(32) NOT NULL,
  `ruleSetUuid` varchar(32) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`) USING BTREE,
  KEY `fkPolicyRouteRuleSetVRouterRefVOVirtualRouteVMVO` (`vRouterUuid`),
  KEY `fkPolicyRouteRuleSetVRouterRefVOPolicyRouteRuleSetVO` (`ruleSetUuid`),
  CONSTRAINT `fkPolicyRouteRuleSetVRouterRefVOVirtualRouteVMVO` FOREIGN KEY (`vRouterUuid`) REFERENCES `VirtualRouterVmVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkPolicyRouteRuleSetVRouterRefVOPolicyRouteRuleSetVO` FOREIGN KEY (`ruleSetUuid`) REFERENCES `PolicyRouteRuleSetVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PolicyRouteRuleSetL3RefVO` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `ruleSetUuid` varchar(32) NOT NULL,
  `l3NetworkUuid` varchar(32) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`) USING BTREE,
  KEY `fkPolicyRouteRuleSetNicRefVOPolicyRouteRuleSetVO` (`ruleSetUuid`) USING BTREE,
  KEY `fkPolicyRouteRuleSetNicRefVOVmNicVO` (`l3NetworkUuid`) USING BTREE,
  CONSTRAINT `fkPolicyRouteRuleSetNicRefVOVmNicVO` FOREIGN KEY (`l3NetworkUuid`) REFERENCES `L3NetworkEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkPolicyRouteRuleSetNicRefVOPolicyRouteRuleSetVO` FOREIGN KEY (`ruleSetUuid`) REFERENCES `PolicyRouteRuleSetVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `TicketTypeVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `requests` VARCHAR(2048) NOT NULL,
    `type` VARCHAR(255) NOT NULL,
    `adminOnly` tinyint(1) unsigned NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `TicketTypeTicketFlowCollectionRefVO` (
    `ticketTypeUuid` VARCHAR(32) NOT NULL,
    `ticketFlowCollectionUuid` VARCHAR(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`ticketTypeUuid`,`ticketFlowCollectionUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`TicketVO` ADD COLUMN ticketTypeUuid VARCHAR(32) DEFAULT NULL;
ALTER TABLE `zstack`.`TicketVO` ADD CONSTRAINT `fkTicketVOTicketTypeVO` FOREIGN KEY (`ticketTypeUuid`) REFERENCES `TicketTypeVO` (`uuid`) ON DELETE SET NULL;

ALTER TABLE `zstack`.`ArchiveTicketVO` ADD COLUMN ticketTypeUuid VARCHAR(32) DEFAULT NULL;
UPDATE `zstack`.`ArchiveTicketVO` SET ticketTypeUuid = '3b933e9aaf2d49b9a3dcf0c92867790f' WHERE ticketTypeUuid is NULL;

INSERT INTO ResourceVO (`uuid`, `resourceName`, `resourceType`, `concreteResourceType`) VALUES ('3b933e9aaf2d49b9a3dcf0c92867790f', 'CREATE_VM_INSTANCE_TICKET_TYPE', 'TicketTypeVO', 'org.zstack.ticket.entity.TicketTypeVO');

INSERT INTO zstack.TicketTypeVO (`uuid`, `name`, `requests`, `type`, `adminOnly`, `createDate`, `lastOpDate`)
VALUES ('3b933e9aaf2d49b9a3dcf0c92867790f', 'CREATE_VM_INSTANCE_TICKET_TYPE', '', 'CREATE_VM_INSTANCE_TICKET_TYPE', 0, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

UPDATE zstack.TicketVO SET ticketTypeUuid = '3b933e9aaf2d49b9a3dcf0c92867790f' WHERE ticketTypeUuid is NULL;

INSERT INTO zstack.TicketTypeTicketFlowCollectionRefVO (`ticketTypeUuid`, `ticketFlowCollectionUuid`, `lastOpDate`, `createDate`)
SELECT '3b933e9aaf2d49b9a3dcf0c92867790f', `uuid`, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP() from TicketFlowCollectionVO where uuid != '872c04e82fee40509447b9ec90fc5aa1';

CREATE TABLE `IAM2OrganizationProjectRefVO` (
`id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
`projectUuid` varchar(32) NOT NULL,
`organizationUuid` varchar(32) NOT NULL,
`lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
`createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
PRIMARY KEY (`id`),
UNIQUE KEY `projectUuid` (`projectUuid`)
) ENGINE=InnoDB AUTO_INCREMENT=3624 DEFAULT CHARSET=utf8;

DELETE FROM IAM2OrganizationAttributeVO WHERE `name` = '__OrganizationSupervisor__' and `value` not in (SELECT `uuid` FROM IAM2VirtualIDVO);

-- Fixes ZSTAC-22582
UPDATE `zstack`.`GlobalConfigVO` SET `description`='qcow2 allocation policy, can be none, metadata', `defaultValue`='metadata' WHERE `category`='sharedblock' AND `name`='qcow2.allocation';
UPDATE `zstack`.`GlobalConfigVO` SET `value`='metadata' WHERE `category`='sharedblock' AND `name`='qcow2.allocation' AND (`value`='full' OR `value`='falloc');
UPDATE `zstack`.`TemplateConfigVO` SET `defaultValue`='metadata' WHERE `category`='sharedblock' AND `name`='qcow2.allocation';
UPDATE `zstack`.`TemplateConfigVO` SET `value`='metadata' WHERE `category`='sharedblock' AND `name`='qcow2.allocation' AND (`value`='full' OR `value`='falloc');

CREATE TABLE `zstack`.`RaidControllerVO` (
    `uuid` varchar(32) not null unique,
    `name` varchar(255) default null,
    `sasAddress` varchar(255) default null,
    `hostUuid` varchar(32) default null,
    `description` varchar(255) default null,
    `productName` varchar(255) default null,
    `adapterNumber` smallint default null,
    `lastOpDate` timestamp not null default '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp not null default '0000-00-00 00:00:00',
    CONSTRAINT fkRaidControllerVOHostVO FOREIGN KEY (hostUuid) REFERENCES HostEO (uuid) ON DELETE CASCADE,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `zstack`.`RaidPhysicalDriveVO` (
    `uuid` varchar(32) not null unique,
    `raidControllerUuid` varchar(32) not null,
    `raidLevel` varchar(32) default null,
    `name` varchar(255) default null,
    `description` varchar(255) default null,
    `deviceModel` varchar(255) default null,
    `enclosureDeviceId` smallint not null,
    `slotNumber` smallint not null,
    `deviceId` smallint default null,
    `diskGroup` smallint default null,
    `wwn` varchar(255) default null,
    `serialNumber` varchar(255) default null,
    `size` bigint(20) not null,
    `driveState` varchar(255) default null,
    `locateStatus` varchar(32) default null,
    `driveType` varchar(255) default null,
    `mediaType` varchar(255) default null,
    `rotationRate` smallint default null,
    `lastOpDate` timestamp not null default '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp not null default '0000-00-00 00:00:00',
    CONSTRAINT fkRaidPhysicalDriveVORaidControllerVO FOREIGN KEY (raidControllerUuid) REFERENCES RaidControllerVO (uuid) ON DELETE CASCADE,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `zstack`.`PhysicalDriveSmartSelfTestHistoryVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `raidPhysicalDriveUuid` varchar(32) default null,
    `runningState` varchar(255) default null,
    `testResult` varchar(255) default null,
    `lastOpDate` timestamp not null default '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp not null default '0000-00-00 00:00:00',
    CONSTRAINT fkPhysicalDriveSmartSelfTestHistoryVORaidPhysicalDriveVO FOREIGN KEY (raidPhysicalDriveUuid) REFERENCES RaidPhysicalDriveVO (uuid) ON DELETE CASCADE,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VpcFirewallVO` (
  `uuid` varchar(32) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VpcFirewallRuleSetVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `vyosName` varchar(28) NOT NULL,
  `vpcFirewallUuid` varchar(32) NOT NULL,
  `actionType` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `enableDefaultLog` tinyint(1) NOT NULL DEFAULT '0',
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `isDefault` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`) USING BTREE,
  KEY `fkVpcFirewallRuleSetVOVpcFirewallVO` (`vpcFirewallUuid`),
  CONSTRAINT `fkVpcFirewallRuleSetVOVpcFirewallVO` FOREIGN KEY (`vpcFirewallUuid`) REFERENCES `VpcFirewallVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VpcFirewallRuleVO` (
  `uuid` varchar(32) NOT NULL,
  `vpcFirewallUuid` varchar(32) NOT NULL,
  `ruleSetUuid` varchar(32) NOT NULL,
  `ruleSetName` varchar(255) NOT NULL,
  `action` varchar(255) NOT NULL,
  `protocol` varchar(255) DEFAULT NULL,
  `sourcePort` varchar(255) DEFAULT NULL,
  `destPort` varchar(255) DEFAULT NULL,
  `sourceIp` varchar(255) DEFAULT NULL,
  `destIp` varchar(255) DEFAULT NULL,
  `ruleNumber` int(10) NOT NULL,
  `icmpTypeName` varchar(255) DEFAULT NULL,
  `allowStates` varchar(255) DEFAULT NULL,
  `tcpFlag` varchar(255) DEFAULT NULL,
  `enableLog` tinyint(1) NOT NULL DEFAULT '0',
  `state` varchar(32) NOT NULL DEFAULT '0',
  `isDefault` tinyint(1) NOT NULL DEFAULT '0',
  `description` varchar(2048) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`) USING BTREE,
  KEY `fkVpcFirewallRuleVOVpcFirewallVO` (`vpcFirewallUuid`),
  KEY `fkVpcFirewallRuleVOVpcFirewallRuleSetVO` (`ruleSetUuid`),
  CONSTRAINT `fkVpcFirewallRuleVOVpcFirewallRuleSetVO` FOREIGN KEY (`ruleSetUuid`) REFERENCES `VpcFirewallRuleSetVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkVpcFirewallRuleVOVpcFirewallVO` FOREIGN KEY (`vpcFirewallUuid`) REFERENCES `VpcFirewallVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VpcFirewallRuleSetL3RefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `ruleSetUuid` varchar(32) NOT NULL,
  `l3NetworkUuid` varchar(32) NOT NULL,
  `vpcFirewallUuid` varchar(32) NOT NULL,
  `packetsForwardType` varchar(32) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`) USING BTREE,
  KEY `fkVpcFirewallRuleSetL3RefVOL3NetworkEO` (`l3NetworkUuid`) USING BTREE,
  KEY `fkVpcFirewallRuleSetL3RefVOVpcFirewallRuleSetVO` (`ruleSetUuid`) USING BTREE,
  KEY `fkVpcFirewallRuleSetL3RefVOVpcFirewallVO` (`vpcFirewallUuid`) USING BTREE,
  CONSTRAINT `fkVpcFirewallRuleSetL3RefVOL3NetworkEO` FOREIGN KEY (`l3NetworkUuid`) REFERENCES `L3NetworkEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkVpcFirewallRuleSetL3RefVOVpcFirewallRuleSetVO` FOREIGN KEY (`ruleSetUuid`) REFERENCES `VpcFirewallRuleSetVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkVpcFirewallRuleSetL3RefVOVpcFirewallVO` FOREIGN KEY (`vpcFirewallUuid`) REFERENCES `VpcFirewallVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=50 DEFAULT CHARSET=utf8;

CREATE TABLE `VpcFirewallVRouterRefVO` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `vRouterUuid` varchar(32) NOT NULL,
  `vpcFirewallUuid` varchar(32) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`) USING BTREE,
  KEY `VpcFirewallVRouterRefVOVpcFirewallVO` (`vpcFirewallUuid`),
  KEY `VpcFirewallVRouterRefVOVirtualRouteVmVO` (`vRouterUuid`),
  CONSTRAINT `VpcFirewallVRouterRefVOVpcFirewallVO` FOREIGN KEY (`vpcFirewallUuid`) REFERENCES `VpcFirewallVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `VpcFirewallVRouterRefVOVirtualRouteVmVO` FOREIGN KEY (`vRouterUuid`) REFERENCES `VirtualRouterVmVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

UPDATE `RolePolicyStatementVO` SET statement = replace(statement, '"org.zstack.header.vpc.**"', '"org.zstack.header.vpc.ha.**","org.zstack.vpc.**"') WHERE `statement` LIKE '%"org.zstack.header.vpc.**"%';
