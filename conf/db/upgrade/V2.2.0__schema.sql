# add network category for ZSTAC-6844
DELIMITER $$
CREATE PROCEDURE generateNetworkCategory()
  BEGIN
    DECLARE l3Uuid varchar(32);
    DECLARE l3System tinyint(3) unsigned;
    DECLARE tagUuid varchar(32);
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR SELECT uuid, system FROM zstack.L3NetworkEO;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    OPEN cur;
    read_loop: LOOP
      FETCH cur INTO l3Uuid, l3System;
      IF done THEN
        LEAVE read_loop;
      END IF;
      SET tagUuid = REPLACE(UUID(), '-', '');

      IF l3System = 1
      THEN
        UPDATE zstack.L3NetworkEO SET system = 0 WHERE uuid = l3Uuid;
        INSERT zstack.SystemTagVO(uuid, resourceUuid, resourceType, inherent, type, tag, lastOpDate, createDate)
          value (tagUuid, l3Uuid, 'L3NetworkVO', 0, 'System', 'networkCategory::Public', NOW(), NOW());
      ELSE
        INSERT zstack.SystemTagVO(uuid, resourceUuid, resourceType, inherent, type, tag, lastOpDate, createDate)
          value (tagUuid, l3Uuid, 'L3NetworkVO', 0, 'System', 'networkCategory::Private', NOW(), NOW());
      END IF;

    END LOOP;
    CLOSE cur;
    # work around a bug of mysql : jira.mariadb.org/browse/MDEV-4602
    SELECT CURTIME();
  END $$
DELIMITER ;

CALL generateNetworkCategory();
DROP PROCEDURE IF EXISTS generateNetworkCategory;