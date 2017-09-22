ALTER TABLE `L3NetworkEO` ADD COLUMN `category` varchar(255) NOT NULL DEFAULT 'Private' COMMENT 'the type network used for';

DROP VIEW IF EXISTS `zstack`.`L3NetworkVO`;
CREATE VIEW `zstack`.`L3NetworkVO` AS SELECT uuid, name, description, state, type, zoneUuid, l2NetworkUuid, system, dnsDomain, createDate, lastOpDate, category FROM `zstack`.`L3NetworkEO` WHERE deleted IS NULL;

# add network category for ZSTAC-6844
DELIMITER $$
CREATE PROCEDURE generateNetworkCategory()
    BEGIN
        DECLARE l3Uuid varchar(32);
        DECLARE l3System tinyint(3) unsigned;
        DECLARE done INT DEFAULT FALSE;
        DECLARE cur CURSOR FOR SELECT uuid, system FROM zstack.L3NetworkEO;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO l3Uuid, l3System;
            IF done THEN
                LEAVE read_loop;
            END IF;

            IF l3System = 1
            THEN
                UPDATE zstack.L3NetworkEO SET system = 0 WHERE uuid = l3Uuid;
                UPDATE zstack.L3NetworkEO SET category = 'Public' WHERE uuid = l3Uuid;
            ELSE
                UPDATE zstack.L3NetworkEO SET category = 'Private' WHERE uuid = l3Uuid;
            END IF;

        END LOOP;
        CLOSE cur;
        # work around a bug of mysql : jira.mariadb.org/browse/MDEV-4602
        SELECT CURTIME();
    END $$
DELIMITER ;

CALL generateNetworkCategory();
DROP PROCEDURE IF EXISTS generateNetworkCategory;

DELIMITER $$
CREATE PROCEDURE addServiceToPublicNetwork()
    BEGIN
        DECLARE l3Uuid VARCHAR(32);
        DECLARE flatUuid VARCHAR(32);
        DECLARE sgUuid VARCHAR(32);
        DECLARE done INT DEFAULT FALSE;
        DECLARE cur CURSOR FOR SELECT uuid FROM zstack.L3NetworkEO WHERE category = 'Public';
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO l3Uuid;
            IF done THEN
                LEAVE read_loop;
            END IF;

            SELECT uuid INTO flatUuid FROM zstack.NetworkServiceProviderVO WHERE type = 'Flat';
            SELECT uuid INTO sgUuid FROM zstack.NetworkServiceProviderVO WHERE type = 'SecurityGroup';

            INSERT INTO NetworkServiceL3NetworkRefVO (`l3NetworkUuid`, `networkServiceProviderUuid`, `networkServiceType`)
              VALUES (l3Uuid, flatUuid, 'Userdata');
            INSERT INTO NetworkServiceL3NetworkRefVO (`l3NetworkUuid`, `networkServiceProviderUuid`, `networkServiceType`)
              VALUES (l3Uuid, flatUuid, 'Eip');
            INSERT INTO NetworkServiceL3NetworkRefVO (`l3NetworkUuid`, `networkServiceProviderUuid`, `networkServiceType`)
              VALUES (l3Uuid, sgUuid, 'SecurityGroup');

        END LOOP;
        CLOSE cur;
        # work around a bug of mysql : jira.mariadb.org/browse/MDEV-4602
        SELECT CURTIME();
    END $$
DELIMITER ;

CALL addServiceToPublicNetwork();
DROP PROCEDURE IF EXISTS addServiceToPublicNetwork;

update PolicyVO set name='SCHEDULER.JOB.CREATE', data='[{\"name\":\"scheduler.job.create\",\"effect\":\"Allow\",\"actions\":[\"scheduler:APICreateSchedulerJobMsg\"]}]' where name='SCHEDULER.CREATE';
update ResourceVO set resourceName='SCHEDULER.JOB.CREATE' where resourceName='SCHEDULER.CREATE';
update PolicyVO set name='SCHEDULER.JOB.UPDATE', data='[{\"name\":\"scheduler.job.update\",\"effect\":\"Allow\",\"actions\":[\"scheduler:APIUpdateSchedulerJobMsg\"]}]' where name='SCHEDULER.UPDATE';
update ResourceVO set resourceName='SCHEDULER.JOB.UPDATE' where resourceName='SCHEDULER.UPDATE';
update PolicyVO set name='SCHEDULER.JOB.DELETE', data='[{\"name\":\"scheduler.job.delete\",\"effect\":\"Allow\",\"actions\":[\"scheduler:APIDeleteSchedulerJobMsg\"]}]' where name='SCHEDULER.DELETE';
update ResourceVO set resourceName='SCHEDULER.JOB.DELETE' where resourceName='SCHEDULER.DELETE';

DELIMITER $$

DROP FUNCTION IF EXISTS `update_policy` $$

CREATE FUNCTION `update_policy` (
    uuid varchar(32),
    policy_name varchar(255),
    policy_data text
) RETURNS VARCHAR(7) CHARSET utf8

BEGIN
    DECLARE policy_uuid varchar(32);
    DECLARE result_string varchar(7);
    SET result_string = 'success';
    SET policy_uuid = REPLACE(UUID(),'-','');

    INSERT INTO PolicyVO (`uuid`, `name`, `accountUuid`, `data`, `lastOpDate`, `createDate`) VALUES (policy_uuid, name, uuid, data, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
    INSERT INTO ResourceVO (`uuid`, `resourceName`, `resourceType`) VALUES (policy_uuid, policy_name, policy_data);

    RETURN(result_string);
END$$

DELIMITER  ;

select update_policy(uuid, 'SCHEDULER.TRIGGER.CREATE', '[{\"name\":\"scheduler.trigger.create\",\"effect\":\"Allow\",\"actions\":[\"scheduler:APICreateSchedulerTriggerMsg\"]}]') from AccountVO where type<>'SystemAdmin';
select update_policy(uuid, 'SCHEDULER.TRIGGER.DELETE', '[{\"name\":\"scheduler.trigger.delete\",\"effect\":\"Allow\",\"actions\":[\"scheduler:APIDeleteSchedulerTriggerMsg\"]}]') from AccountVO where type<>'SystemAdmin';
select update_policy(uuid, 'SCHEDULER.TRIGGER.UPDATE', '[{\"name\":\"scheduler.trigger.update\",\"effect\":\"Allow\",\"actions\":[\"scheduler:APIUpdateSchedulerTriggerMsg\"]}]') from AccountVO where type<>'SystemAdmin';
select update_policy(uuid, 'SCHEDULER.ADD', '[{\"name\":\"scheduler.add\",\"effect\":\"Allow\",\"actions\":[\"scheduler:APIAddSchedulerJobToSchedulerTriggerMsg\"]}]') from AccountVO where type<>'SystemAdmin';
select update_policy(uuid, 'SCHEDULER.REMOVE', '[{\"name\":\"scheduler.remove\",\"effect\":\"Allow\",\"actions\":[\"scheduler:APIRemoveSchedulerJobFromSchedulerTriggerMsg\"]}]') from AccountVO where type<>'SystemAdmin';

CREATE INDEX notification_resource_uuid_idx ON NotificationVO (resourceUuid);
ALTER TABLE `BaremetalHostCfgVO` ADD COLUMN `cloneIso` tinyint(1) unsigned NOT NULL DEFAULT 0;
