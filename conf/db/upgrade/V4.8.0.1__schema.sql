UPDATE `zstack`.`L2NetworkHostRefVO` set `attachStatus` = 'Attached' WHERE `attachStatus` = '3';
UPDATE `zstack`.`L2NetworkHostRefVO` set `attachStatus` = 'Detached' WHERE `attachStatus` != '3';