ALTER TABLE `zstack`.`ConsoleProxyVO` ADD COLUMN `version` varchar(32) DEFAULT NULL;

DELIMITER $$
CREATE PROCEDURE migrateClockTrackSystemTagToGlobalConfig()
BEGIN
    DECLARE vmInstanceUuid VARCHAR(32);
    DECLARE clockTrackTag VARCHAR(32);
    DECLARE clockTrack VARCHAR(32);
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR SELECT systemTag.tag, systemTag.resourceUuid FROM `zstack`.`SystemTagVO` systemTag
     where `tag` like 'clockTrack::%' and `resourceType`='VmInstanceVO';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO clockTrackTag, vmInstanceUuid;
        IF done THEN
            LEAVE read_loop;
        END IF;

        SET clockTrack = SUBSTRING_INDEX(clockTrackTag, '::', -1);
        INSERT INTO zstack.ResourceConfigVO (uuid, name, description, category, value, resourceUuid, resourceType, lastOpDate, createDate)
         values(ruuid, "vm.clock.track", "vm.clock.track", "vm", clockTrack, vmInstanceUuid, "VmInstanceVO", CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
    END LOOP;
    CLOSE cur;
    SELECT CURTIME();
END $$
DELIMITER ;

call migrateClockTrackSystemTagToGlobalConfig();
DROP PROCEDURE IF EXISTS migrateClockTrackSystemTagToGlobalConfig;

DELETE FROM `zstack`.`SystemTagVO` where `tag` like 'clockTrack::%' and `resourceType`='VmInstanceVO';