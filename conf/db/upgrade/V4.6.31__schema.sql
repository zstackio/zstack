ALTER TABLE `zstack`.`KVMHostVO` ADD COLUMN `osDistribution` varchar(64) DEFAULT NULL;
ALTER TABLE `zstack`.`KVMHostVO` ADD COLUMN `osRelease` varchar(64) DEFAULT NULL;
ALTER TABLE `zstack`.`KVMHostVO` ADD COLUMN `osVersion` varchar(64) DEFAULT NULL;

DELIMITER $$
CREATE PROCEDURE moveOsInfoToKVMHostVO()
    BEGIN
        DECLARE hostUuid CHAR(32);
        DECLARE osInfo VARCHAR(64);
        DECLARE done INT DEFAULT FALSE;
        DECLARE curDistribution CURSOR FOR
                SELECT resourceUuid, SUBSTRING(tag, 19) FROM `zstack`.`SystemTagVO` WHERE tag LIKE 'os::distribution::%';
        DECLARE curRelease CURSOR FOR
                SELECT resourceUuid, SUBSTRING(tag, 14) FROM `zstack`.`SystemTagVO` WHERE tag LIKE 'os::release::%';
        DECLARE curVersion CURSOR FOR
                SELECT resourceUuid, SUBSTRING(tag, 14) FROM `zstack`.`SystemTagVO` WHERE tag LIKE 'os::version::%';
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

        OPEN curDistribution;
        read_loop1: LOOP
            FETCH curDistribution INTO hostUuid, osInfo;
            IF done THEN
                LEAVE read_loop1;
            END IF;

            UPDATE `zstack`.`KVMHostVO` SET osDistribution = osInfo WHERE uuid = hostUuid;
        END LOOP;
        CLOSE curDistribution;
        SET done = FALSE;

        OPEN curRelease;
        read_loop2: LOOP
            FETCH curRelease INTO hostUuid, osInfo;
            IF done THEN
                LEAVE read_loop2;
            END IF;

            UPDATE `zstack`.`KVMHostVO` SET osRelease = osInfo WHERE uuid = hostUuid;
        END LOOP;
        CLOSE curRelease;
        SET done = FALSE;

        OPEN curVersion;
        read_loop3: LOOP
            FETCH curVersion INTO hostUuid, osInfo;
            IF done THEN
                LEAVE read_loop3;
            END IF;

            UPDATE `zstack`.`KVMHostVO` SET osVersion = osInfo WHERE uuid = hostUuid;
        END LOOP;
        CLOSE curVersion;
        SELECT CURTIME();
    END $$
DELIMITER ;

call moveOsInfoToKVMHostVO();
DROP PROCEDURE IF EXISTS moveOsInfoToKVMHostVO;

# add default zone and recreate ZoneVO
ALTER TABLE `zstack`.`ZoneEO` ADD COLUMN `isDefault` tinyint(1) unsigned DEFAULT 0;
DROP VIEW IF EXISTS `zstack`.`ZoneVO`;
CREATE VIEW `zstack`.`ZoneVO` AS SELECT uuid, name, type, description, state, isDefault, createDate, lastOpDate FROM `zstack`.`ZoneEO` WHERE deleted IS NULL;
