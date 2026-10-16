CALL ADD_COLUMN('ZnsSegmentRefVO', 'consistencyStatus', 'VARCHAR(32)', 0, 'Unknown');
CALL ADD_COLUMN('ZnsSegmentRefVO', 'consistencyDetails', 'TEXT', 1, NULL);

ALTER TABLE `ZnsSegmentRefVO`
    MODIFY COLUMN `zoneUuid` VARCHAR(32) DEFAULT NULL,
    MODIFY COLUMN `l2NetworkUuid` VARCHAR(32) DEFAULT NULL,
    MODIFY COLUMN `accountUuid` VARCHAR(32) DEFAULT NULL;
