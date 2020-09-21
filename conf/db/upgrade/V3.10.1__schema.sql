UPDATE ResourceConfigVO r, PrimaryStorageVO p SET r.category='localStoragePrimaryStorage' WHERE r.name='qcow2.allocation' and r.resourceUuid=p.uuid and p.type='LocalStorage';
UPDATE ResourceConfigVO r, PrimaryStorageVO p SET r.category='sharedblock', r.description='qcow2 allocation policy, can be none, metadata' WHERE r.name='qcow2.allocation' and r.resourceUuid=p.uuid and p.type='SharedBlock';
UPDATE ResourceConfigVO r, PrimaryStorageVO p SET r.category='nfsPrimaryStorage' WHERE r.name='qcow2.allocation' and r.resourceUuid=p.uuid and p.type='NFS';
UPDATE ResourceConfigVO r, PrimaryStorageVO p SET r.category='sharedMountPointPrimaryStorage' WHERE r.name='qcow2.allocation' and r.resourceUuid=p.uuid and p.type='SharedMountPoint';
UPDATE ResourceConfigVO r, PrimaryStorageVO p SET r.category='ministorage' WHERE r.name='qcow2.allocation' and r.resourceUuid=p.uuid and p.type='MiniStorage';

# upgrade PROJECT_OPERATOR_OF_PROJECT and PROJECT_ADMIN_OF_PROJECT to new data structure
DROP PROCEDURE IF EXISTS upgradeProjectOperatorSystemTags;
DELIMITER $$
CREATE PROCEDURE upgradeProjectOperatorSystemTags()
BEGIN
    DECLARE projectOperatorTag VARCHAR(62);
    DECLARE projectUuid VARCHAR(32);
    DECLARE iam2VirtualIDUuid VARCHAR(32);
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR SELECT systemTag.tag, systemTag.resourceUuid FROM `zstack`.`SystemTagVO` systemTag where systemTag.tag like 'projectOperatorOfProjectUuid::%';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO projectOperatorTag, iam2VirtualIDUuid;
        IF done THEN
            LEAVE read_loop;
        END IF;

        SET projectUuid = SUBSTRING_INDEX(projectOperatorTag, '::', 1);

        INSERT INTO IAM2VirtualIDRoleRefVO (`virtualIDUuid`, `roleUuid`, `projectUuid`) VALUES (iam2VirtualIDUuid, 'f2f474c60e7340c0a1d44080d5bde3a9', projectUuid);
    END LOOP;
    CLOSE cur;
    SELECT CURTIME();
END $$
DELIMITER ;
CALL upgradeProjectOperatorSystemTags();

DROP PROCEDURE IF EXISTS upgradeProjectAdminSystemTags;
DELIMITER $$
CREATE PROCEDURE upgradeProjectAdminSystemTags()
BEGIN
    DECLARE projectAdminTag VARCHAR(59);
    DECLARE projectUuid VARCHAR(32);
    DECLARE iam2VirtualIDUuid VARCHAR(32);
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR SELECT systemTag.tag, systemTag.resourceUuid FROM `zstack`.`SystemTagVO` systemTag where systemTag.tag like 'projectAdminOfProjectUuid::%';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO projectAdminTag, iam2VirtualIDUuid;
        IF done THEN
            LEAVE read_loop;
        END IF;

        SET projectUuid = SUBSTRING_INDEX(projectAdminTag, '::', 1);

        INSERT INTO IAM2VirtualIDRoleRefVO (`virtualIDUuid`, `roleUuid`, `projectUuid`) VALUES (iam2VirtualIDUuid, 'f2f474c60e7340c0a1d44080d5bde3a9', projectUuid);
    END LOOP;
    CLOSE cur;
    SELECT CURTIME();
END $$
DELIMITER ;
CALL upgradeProjectAdminSystemTags();

CREATE TABLE `IAM2ProjectRoleVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `iam2ProjectRoleType` VARCHAR(64) NOT NULL,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE IAM2VirtualIDRoleRefVO DROP FOREIGN KEY fkIAM2VirtualIDRoleRefVOIAM2VirtualIDVO;
ALTER TABLE IAM2VirtualIDRoleRefVO DROP FOREIGN KEY fkIAM2VirtualIDRoleRefVORoleVO;
ALTER TABLE IAM2VirtualIDRoleRefVO ADD COLUMN `targetAccountUuid` varchar(32) NOT NULL;
ALTER TABLE IAM2VirtualIDRoleRefVO DROP PRIMARY KEY, ADD PRIMARY KEY(virtualIDUuid, roleUuid, targetAccountUuid);
ALTER TABLE IAM2VirtualIDRoleRefVO ADD CONSTRAINT fkIAM2VirtualIDRoleRefVOIAM2VirtualIDVO FOREIGN KEY (virtualIDUuid) REFERENCES IAM2VirtualIDVO (uuid) ON DELETE CASCADE;
ALTER TABLE IAM2VirtualIDRoleRefVO ADD CONSTRAINT fkIAM2VirtualIDRoleRefVORoleVO FOREIGN KEY (roleUuid) REFERENCES RoleVO (uuid) ON DELETE CASCADE;
CREATE INDEX idxIAM2VirtualIDRoleRefVOTargetAccountUuid ON IAM2VirtualIDRoleRefVO (targetAccountUuid);
