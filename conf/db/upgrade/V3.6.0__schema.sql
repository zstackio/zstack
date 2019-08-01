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

-- upgrade privilege admin
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
