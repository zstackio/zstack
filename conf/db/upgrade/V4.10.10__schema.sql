CALL INSERT_COLUMN('AlarmRecordsVO', 'operatorAccountUuid', 'char(32)', 1, NULL, 'readStatus');
CALL INSERT_COLUMN('EventRecordsVO', 'operatorAccountUuid', 'char(32)', 1, NULL, 'readStatus');

UPDATE `zstack`.`ResourceVO` set `resourceType` = 'ThirdPartyAccountSourceVO' where uuid in (select uuid from ThirdPartyAccountSourceVO);

UPDATE `zstack`.`AlarmVO` SET `name` = 'Data Storage Available Capacity' WHERE `name` = 'Primary Storage Available Capacity';
UPDATE `zstack`.`AlarmVO` SET `name` = 'Data Storage Available Physical Capacity' WHERE `name` = 'Primary Storage Available Physical Capacity';

DELETE FROM `zstack`.`ResourceConfigVO` WHERE `category`='sharedblock' AND `name`='qcow2.allocation';
