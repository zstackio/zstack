DROP PROCEDURE IF EXISTS addLongJobVOIndex;

DELIMITER $$
CREATE PROCEDURE addLongJobVOIndex()
BEGIN
    IF NOT EXISTS (SELECT * FROM information_schema.statistics WHERE table_schema = 'zstack' AND table_name = "LongJobVO" AND index_name = "idxLongJobVOtargetResourceUuid") THEN
        CREATE INDEX idxLongJobVOtargetResourceUuid ON LongJobVO (targetResourceUuid);
    END IF;
END $$
DELIMITER ;

CALL addLongJobVOIndex();
