--INSERT INTO `zstack`.`IAM2OrganizationVO` (uuid, name, state, type, srcType, createDate, lastOpDate, rootOrganizationUuid)
-- values ('6e3d19dab98348d8bd67657378843f82', 'default', 'Enabled', 'Default', 'ZStack', NOW(), NOW(), '6e3d19dab98348d8bd67657378843f82');
--INSERT INTO `zstack`.`ResourceVO`(uuid, resourceName, resourceType, concreteResourceType) values ('6e3d19dab98348d8bd67657378843f82','default', 'IAM2OrganizationVO', 'org.zstack.iam2.entity.IAM2OrganizationVO');
--INSERT INTO `zstack`.`AccountResourceRefVO`(accountUuid, ownerAccountUuid, resourceUuid, resourceType, permission, isShared, concreteResourceType)  values ('36c27e8ff05c4780bf6d2fa65700f22e', '36c27e8ff05c4780bf6d2fa65700f22e', '6e3d19dab98348d8bd67657378843f82', 'IAM2OrganizationVO', 2, false, 'org.zstack.iam2.entity.IAM2OrganizationVO');

DELIMITER $$
CREATE PROCEDURE insertDefaultIAM2Organization()
BEGIN
    DECLARE virtualIDUuid VARCHAR(32);
    DECLARE organizationUuid VARCHAR(32);
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR SELECT uuid, '6e3d19dab98348d8bd67657378843f82' FROM zstack.IAM2VirtualIDVO where type = 'ZStack' and uuid not in (SELECT virtualIDUuid FROM zstack.IAM2VirtualIDOrganizationRefVO);
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO virtualIDUuid, organizationUuid;
        IF done THEN
            LEAVE read_loop;
        END IF;
        INSERT INTO `zstack`.IAM2VirtualIDOrganizationRefVO (virtualIDUuid, organizationUuid) VALUES (virtualIDUuid, organizationUuid);

    END LOOP;
    CLOSE cur;
    SELECT CURTIME();
END $$
DELIMITER ;

CALL insertDefaultIAM2Organization();
DROP PROCEDURE IF EXISTS insertDefaultIAM2Organization;


CREATE TABLE `IAM2ProjectVirtualIDGroupRefVO` (
    `groupUuid` VARCHAR(32) NOT NULL,
    `projectUuid` VARCHAR(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`groupUuid`,`projectUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DELIMITER $$
CREATE PROCEDURE insertIAM2ProjectVirtualIDGroupRef()
BEGIN
    DECLARE groupUuid VARCHAR(32);
    DECLARE projectUuid VARCHAR(32);
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR SELECT uuid, projectUuid  FROM zstack.IAM2VirtualIDGroupVO;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO groupUuid, projectUuid;
        IF done THEN
            LEAVE read_loop;
        END IF;
        INSERT INTO `zstack`.IAM2ProjectVirtualIDGroupRefVO (groupUuid, projectUuid) VALUES (groupUuid, projectUuid);

    END LOOP;
    CLOSE cur;
    SELECT CURTIME();
END $$
DELIMITER ;

CALL insertIAM2ProjectVirtualIDGroupRef();
DROP PROCEDURE IF EXISTS insertIAM2ProjectVirtualIDGroupRef;