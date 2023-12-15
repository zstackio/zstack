UPDATE `zstack`.`L2NetworkHostRefVO` set `attachStatus` = 'Detached' WHERE `attachStatus` = '0' or `attachStatus` = '1' or `attachStatus` = '2';
UPDATE `zstack`.`L2NetworkHostRefVO` set `attachStatus` = 'Attached' WHERE `attachStatus` = '3';

ALTER TABLE ConsoleProxyVO ADD COLUMN `expiredDate` timestamp NOT NULL;
UPDATE ImageEO SET md5sum = NULL where md5sum != 'not calculated';