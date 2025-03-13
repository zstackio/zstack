DROP PROCEDURE IF EXISTS createThickProvisionVolumeTag;
DELIMITER $$
CREATE PROCEDURE createThickProvisionVolumeTag()
BEGIN
    DECLARE volUuid VARCHAR(32);
    DECLARE newTagUuid VARCHAR(32);
    DECLARE done INT DEFAULT FALSE;

    DECLARE volCursor CURSOR FOR
        SELECT uuid
        FROM zstack.VolumeVO
        WHERE type = 'Memory'
          AND primaryStorageUuid IN (
            SELECT uuid
            FROM zstack.PrimaryStorageVO
            WHERE type = 'SharedBlock'
        )
          AND uuid NOT IN (
            SELECT resourceUuid
            FROM zstack.SystemTagVO
            WHERE resourceType = 'VolumeVO'
              AND tag = 'volumeProvisioningStrategy::ThickProvisioning'
        );

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN volCursor;

    read_loop:
    LOOP
        FETCH volCursor INTO volUuid;
        IF done THEN
            LEAVE read_loop;
        END IF;

        SET newTagUuid = REPLACE(UUID(), '-', '');

        INSERT INTO zstack.SystemTagVO (uuid, resourceUuid, resourceType, inherent, type, tag, createDate, lastOpDate)
        VALUES (newTagUuid, volUuid, 'VolumeVO', 0, 'System', 'volumeProvisioningStrategy::ThickProvisioning', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
    END LOOP;

    CLOSE volCursor;
    SELECT CURTIME() AS finishTime;
END $$
DELIMITER ;

CALL createThickProvisionVolumeTag();
DROP PROCEDURE IF EXISTS createThickProvisionVolumeTag;

UPDATE `zstack`.`VolumeSnapshotTreeVO` t JOIN `zstack`.`VolumeVO` v ON t.volumeUuid = v.uuid
SET t.rootImageUuid = v.rootImageUuid
WHERE t.current = true
  AND v.rootImageUuid IS NOT NULL
  AND t.rootImageUuid IS NULL;
