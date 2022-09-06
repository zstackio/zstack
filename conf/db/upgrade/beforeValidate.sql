-- Update `schema_version' since we've changed SQL to match the expectation
-- of the newer version MariaDB.
--
-- WARNING: `schema_version' table doesn't exist on a clean environment, thus
-- after 'flyway clean' we prepared an empty table after 'flyway baseline'.
DELIMITER $$

DROP PROCEDURE IF EXISTS `zstack`.`update_schema_checksum` $$

CREATE PROCEDURE `zstack`.`update_schema_checksum`()
BEGIN
    IF EXISTS(SELECT table_name FROM information_schema.tables WHERE table_name = 'schema_version')
    THEN
        update `zstack`.`schema_version` set `checksum`=1083194846  where `script`='V1.6__schema.sql'   and `checksum` <> 1083194846;
        update `zstack`.`schema_version` set `checksum`=-1569422253 where `script`='V2.1.0__schema.sql' and `checksum` <> -1569422253;
        update `zstack`.`schema_version` set `checksum`=1564279419  where `script`='V3.0.0__schema.sql' and `checksum` <> 1564279419;
        update `zstack`.`schema_version` set `checksum`=565652311   where `script`='V3.7.2__schema.sql' and `checksum` <> 565652311;
        update `zstack`.`schema_version` set `checksum`=-143027462   where `script`='V3.9.1__schema.sql' and `checksum` <> -143027462;
        update `zstack`.`schema_version` set `checksum`=468508308   where `script`='V4.0.0__schema.sql' and `checksum` <> 468508308;
        update `zstack`.`schema_version` set `checksum`=-1809609423   where `script`='V4.2.32__schema.sql' and `checksum` <> -1809609423;
    END IF;
END $$

DELIMITER  ;

CALL `zstack`.update_schema_checksum();
DROP PROCEDURE IF EXISTS `zstack`.update_schema_checksum;
