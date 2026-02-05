-- Add prefixLen column to UsedIpVO for IPv6 addresses outside IP range
CALL ADD_COLUMN('UsedIpVO', 'prefixLen', 'INT', 1, NULL);

-- Backfill prefixLen from IpRangeVO for existing IPv6 UsedIpVO records
UPDATE `UsedIpVO` `u`
INNER JOIN `IpRangeVO` `r` ON `u`.`ipRangeUuid` = `r`.`uuid`
SET `u`.`prefixLen` = `r`.`prefixLen`
WHERE `u`.`ipVersion` = 6
  AND `u`.`ipRangeUuid` IS NOT NULL
  AND `u`.`prefixLen` IS NULL;

-- Modify ipRangeUuid foreign key constraint to SET NULL on delete (instead of CASCADE)
-- This allows UsedIpVO records to exist without an IpRange (for IPs outside range)
DELIMITER $$

CREATE PROCEDURE ModifyUsedIpVOForeignKey()
BEGIN
    DECLARE constraint_exists INT;

    -- Check if the constraint exists
    SELECT COUNT(*)
    INTO constraint_exists
    FROM `INFORMATION_SCHEMA`.`TABLE_CONSTRAINTS`
    WHERE `TABLE_SCHEMA` = 'zstack'
      AND `TABLE_NAME` = 'UsedIpVO'
      AND `CONSTRAINT_NAME` = 'fkUsedIpVOIpRangeEO';

    IF constraint_exists > 0 THEN
        -- Drop the existing constraint
        ALTER TABLE `zstack`.`UsedIpVO` DROP FOREIGN KEY `fkUsedIpVOIpRangeEO`;

        -- Re-create with SET NULL on delete
        ALTER TABLE `zstack`.`UsedIpVO`
        ADD CONSTRAINT `fkUsedIpVOIpRangeEO`
        FOREIGN KEY (`ipRangeUuid`) REFERENCES `IpRangeEO`(`uuid`) ON DELETE SET NULL;
    END IF;
END $$

DELIMITER ;

CALL ModifyUsedIpVOForeignKey();
DROP PROCEDURE IF EXISTS ModifyUsedIpVOForeignKey;
