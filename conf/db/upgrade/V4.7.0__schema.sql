CREATE TABLE IF NOT EXISTS `zstack`.`HaStrategyConditionVO` (
    `uuid` varchar(32) NOT NULL,
    `name` varchar(256),
    `strategyCondition` varchar(256) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DELIMITER $$
CREATE PROCEDURE addHaStrategyConditionOnVmHaLevel()
    BEGIN
        DECLARE isHaEnable VARCHAR(32);
        DECLARE fencerStrategy VARCHAR(32);
        DECLARE resourceUuid VARCHAR(32);
        DECLARE current_time_stamp timestamp;
        DECLARE done INT DEFAULT FALSE;
        DECLARE haCursor CURSOR FOR select value from GlobalConfigVO where category = 'ha' and name = 'enable';
        DECLARE fencerCursor CURSOR for select value from GlobalConfigVO where category = 'ha' and name = 'self.fencer.strategy';
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
        OPEN haCursor;
        OPEN fencerCursor;
        read_loop: LOOP
            FETCH haCursor INTO isHaEnable;
            FETCH fencerCursor INTO fencerStrategy;
            IF done THEN
                LEAVE read_loop;
            END IF;

            SET resourceUuid = (REPLACE(UUID(), '-', ''));
            SET current_time_stamp = current_timestamp();
            IF (LOWER(isHaEnable) = 'false' OR fencerStrategy = 'Permissive') THEN
                INSERT INTO HaStrategyConditionVO(`uuid`, `name`, `strategyCondition`, `lastOpDate`, `createDate`) values (resourceUuid, 'ha strategy condition', '{"hostStorageStatue":false,"hostBusinessNic":false}', current_time_stamp, current_time_stamp);
            ELSE
                INSERT INTO HaStrategyConditionVO(`uuid`, `name`, `strategyCondition`, `lastOpDate`, `createDate`) values (resourceUuid, 'ha strategy condition', '{"hostStorageStatue":true,"hostBusinessNic":false}', current_time_stamp, current_time_stamp);
            END IF;
        END LOOP;
        CLOSE haCursor;
        CLOSE fencerCursor;
        SELECT CURTIME();
    END $$
DELIMITER ;

call addHaStrategyConditionOnVmHaLevel();
DROP PROCEDURE IF EXISTS addHaStrategyConditionOnVmHaLevel;

CREATE TABLE IF NOT EXISTS `zstack`.`HostHaStateVO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'host uuid',
    `state` varchar(32),
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DELIMITER $$
CREATE PROCEDURE addHostHaStatus()
    BEGIN
        DECLARE isHaEnable VARCHAR(32);
        DECLARE hostUuid VARCHAR(32);
        DECLARE current_time_stamp timestamp;
        DECLARE done INT DEFAULT FALSE;
        DECLARE haCursor CURSOR FOR select uuid from HostVO;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
        OPEN haCursor;
        read_loop: LOOP
            FETCH haCursor INTO hostUuid;
            IF done THEN
                LEAVE read_loop;
            END IF;

            SET current_time_stamp = current_timestamp();
            insert into HostHaStateVO(`uuid`, `state`,`lastOpDate`, `createDate`)values (hostUuid, 'None', current_time_stamp, current_time_stamp);
        END LOOP;
        CLOSE haCursor;
        SELECT CURTIME();
    END $$
DELIMITER ;

call addHostHaStatus();
DROP PROCEDURE IF EXISTS addHostHaStatus;