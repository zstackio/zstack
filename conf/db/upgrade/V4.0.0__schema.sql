ALTER TABLE `zstack`.`TicketStatusHistoryVO` ADD COLUMN `sequence` INT;
ALTER TABLE `zstack`.`ArchiveTicketStatusHistoryVO` ADD COLUMN `sequence` INT;

DROP PROCEDURE IF EXISTS updateTicketStatusHistoryVO;

DELIMITER $$
CREATE PROCEDURE updateTicketStatusHistoryVO()
BEGIN
    DECLARE sequence INT;
    DECLARE uuid VARCHAR(32);
    DECLARE done INT DEFAULT FALSE;
    DECLARE loopCount INT DEFAULT 1;
    DECLARE cur CURSOR FOR SELECT history.sequence,history.uuid FROM `zstack`.`TicketStatusHistoryVO` history WHERE history.fromStatus != 'FinalApproved' ORDER BY history.createDate,history.operationContextType;
    DECLARE extra_cur CURSOR FOR SELECT history.sequence,history.uuid FROM `zstack`.`TicketStatusHistoryVO` history WHERE history.fromStatus = 'FinalApproved' ORDER BY history.createDate,history.operationContextType;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cur;
    update_loop: LOOP
        FETCH cur INTO sequence,uuid;
        IF done THEN
            LEAVE update_loop;
        END IF;

        UPDATE `zstack`.`TicketStatusHistoryVO` history SET history.sequence = loopCount WHERE history.uuid = uuid;
        SET loopCount = loopCount + 1;
    END LOOP;
    CLOSE cur;

    SET done = FALSE;
    OPEN extra_cur;
    extra_loop: LOOP
        FETCH extra_cur INTO sequence,uuid;
        IF done THEN
            LEAVE extra_loop;
        END IF;

        UPDATE `zstack`.`TicketStatusHistoryVO` history SET history.sequence = loopCount WHERE history.uuid = uuid;
        SET loopCount = loopCount + 1;
    END LOOP;
    CLOSE extra_cur;

END $$
DELIMITER ;

DROP PROCEDURE IF EXISTS updateArchiveTicketStatusHistoryVO;

DELIMITER $$
CREATE PROCEDURE updateArchiveTicketStatusHistoryVO()
BEGIN
    DECLARE sequence INT;
    DECLARE uuid VARCHAR(32);
    DECLARE done INT DEFAULT FALSE;
    DECLARE loopCount INT DEFAULT 1;
    DECLARE cur CURSOR FOR SELECT history.sequence,history.uuid FROM `zstack`.`ArchiveTicketStatusHistoryVO` history WHERE history.fromStatus != 'FinalApproved' ORDER BY history.createDate,history.operationContextType;
    DECLARE extra_cur CURSOR FOR SELECT history.sequence,history.uuid FROM `zstack`.`ArchiveTicketStatusHistoryVO` history WHERE history.fromStatus = 'FinalApproved' ORDER BY history.createDate,history.operationContextType;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cur;
    update_loop: LOOP
        FETCH cur INTO sequence,uuid;
        IF done THEN
            LEAVE update_loop;
        END IF;

        UPDATE `zstack`.`ArchiveTicketStatusHistoryVO` history SET history.sequence = loopCount WHERE history.uuid = uuid;
        SET loopCount = loopCount + 1;
    END LOOP;
    CLOSE cur;

    SET done = FALSE;
    OPEN extra_cur;
    extra_loop: LOOP
        FETCH extra_cur INTO sequence,uuid;
        IF done THEN
            LEAVE extra_loop;
        END IF;

        UPDATE `zstack`.`ArchiveTicketStatusHistoryVO` history SET history.sequence = loopCount WHERE history.uuid = uuid;
        SET loopCount = loopCount + 1;
    END LOOP;
    CLOSE extra_cur;

END $$
DELIMITER ;

call updateTicketStatusHistoryVO();
DROP PROCEDURE IF EXISTS updateTicketStatusHistoryVO;
call updateArchiveTicketStatusHistoryVO();
DROP PROCEDURE IF EXISTS updateArchiveTicketStatusHistoryVO;

ALTER TABLE `zstack`.`TicketStatusHistoryVO` CHANGE sequence sequence INT AUTO_INCREMENT UNIQUE;
ALTER TABLE `zstack`.`ArchiveTicketStatusHistoryVO` CHANGE sequence sequence INT AUTO_INCREMENT UNIQUE;