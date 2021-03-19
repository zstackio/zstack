DROP PROCEDURE IF EXISTS upgradeProjectOperatorSystemTags;
DELIMITER $$
CREATE PROCEDURE upgradeProjectOperatorSystemTags()
BEGIN
    DECLARE projectOperatorTag VARCHAR(62);
    DECLARE targetProjectUuid VARCHAR(32);
    DECLARE iameTargetAccountUuid VARCHAR(32);
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

        SET targetProjectUuid = SUBSTRING_INDEX(projectOperatorTag, '::', -1);
        SELECT `accountUuid` into iameTargetAccountUuid FROM `IAM2ProjectAccountRefVO` WHERE `projectUuid` = targetProjectUuid LIMIT 1;
        IF (select count(*) from IAM2VirtualIDRoleRefVO where virtualIDUuid = iam2VirtualIDUuid and roleUuid = 'f2f474c60e7340c0a1d44080d5bde3a9' and targetAccountUuid = iameTargetAccountUuid) < 1 THEN
        begin
            INSERT INTO IAM2VirtualIDRoleRefVO (`virtualIDUuid`, `roleUuid`, `targetAccountUuid`, createDate, lastOpDate) VALUES (iam2VirtualIDUuid, 'f2f474c60e7340c0a1d44080d5bde3a9', iameTargetAccountUuid, NOW(), NOW());
        end;
        END IF;
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
    DECLARE targetProjectUuid VARCHAR(32);
    DECLARE iameTargetAccountUuid VARCHAR(32);
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

        SET targetProjectUuid = SUBSTRING_INDEX(projectAdminTag, '::', -1);
        SELECT `accountUuid` into iameTargetAccountUuid FROM `IAM2ProjectAccountRefVO` WHERE `projectUuid` = targetProjectUuid LIMIT 1;
        IF (select count(*) from IAM2VirtualIDRoleRefVO where virtualIDUuid = iam2VirtualIDUuid and roleUuid = '55553cefbbfb42468873897c95408a43' and targetAccountUuid = iameTargetAccountUuid) < 1 THEN
        begin
            INSERT INTO IAM2VirtualIDRoleRefVO (`virtualIDUuid`, `roleUuid`, `targetAccountUuid`, createDate, lastOpDate) VALUES (iam2VirtualIDUuid, '55553cefbbfb42468873897c95408a43', iameTargetAccountUuid, NOW(), NOW());
        end;
        END IF;
    END LOOP;
    CLOSE cur;
    SELECT CURTIME();
END $$
DELIMITER ;
CALL upgradeProjectAdminSystemTags();

ALTER TABLE `zstack`.`SNSEmailPlatformVO` modify COLUMN `password` VARCHAR(255) NULL;
ALTER TABLE `zstack`.`SNSEmailPlatformVO` modify COLUMN `username` VARCHAR(255) NULL;


ALTER TABLE `zstack`.`PciDeviceVO` ADD `chooser` varchar(32) DEFAULT 'None';
UPDATE PciDeviceVO SET chooser='None' WHERE vmInstanceUuid IS NULL;
UPDATE PciDeviceVO SET chooser='Device' WHERE vmInstanceUuid IS NOT NULL;

ALTER TABLE `zstack`.`MdevDeviceVO` ADD `chooser` varchar(32) DEFAULT 'None';
UPDATE MdevDeviceVO SET chooser='None' WHERE vmInstanceUuid IS NULL;
UPDATE MdevDeviceVO SET chooser='Device' WHERE vmInstanceUuid IS NOT NULL;

UPDATE VmInstancePciSpecDeviceRefVO AS ref LEFT JOIN PciDeviceVO AS pci
    ON ref.pciDeviceUuid = pci.uuid
    SET chooser='Spec'
    WHERE ref.vmInstanceUuid = pci.vmInstanceUuid;
UPDATE VmInstanceMdevSpecDeviceRefVO AS ref LEFT JOIN MdevDeviceVO AS mdev
    ON ref.mdevDeviceUuid = mdev.uuid
    SET chooser='Spec'
    WHERE ref.vmInstanceUuid = mdev.vmInstanceUuid;
DROP TABLE IF EXISTS VmInstancePciSpecDeviceRefVO;
DROP TABLE IF EXISTS VmInstanceMdevSpecDeviceRefVO;

DELETE FROM SystemTagVO WHERE tag LIKE 'pciDevice::%';
DELETE FROM SystemTagVO WHERE tag LIKE 'mdevDevice::%';
