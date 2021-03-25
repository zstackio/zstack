DELIMITER $$

DROP PROCEDURE IF EXISTS `zstack`.`update_schema_checksum` $$

CREATE PROCEDURE `zstack`.`update_schema_checksum`()
BEGIN
    IF EXISTS(SELECT table_name FROM information_schema.tables WHERE table_name = 'schema_version')
    THEN
        update `zstack`.`schema_version` set `checksum`=286222955   where `script`='V1.3__schema.sql'   and `checksum` <> 286222955;
        update `zstack`.`schema_version` set `checksum`=1083194846  where `script`='V1.6__schema.sql'   and `checksum` <> 1083194846;
        update `zstack`.`schema_version` set `checksum`=390362109   where `script`='V1.7__schema.sql'   and `checksum` <> 390362109;
        update `zstack`.`schema_version` set `checksum`=-267728668  where `script`='V2.1.0__schema.sql' and `checksum` <> -267728668;
        update `zstack`.`schema_version` set `checksum`=1364422951  where `script`='V2.2.0__schema.sql' and `checksum` <> 1364422951;
        update `zstack`.`schema_version` set `checksum`=-1723491154 where `script`='V2.2.1__schema.sql' and `checksum` <> -1723491154;
        update `zstack`.`schema_version` set `checksum`=1275402472  where `script`='V2.4.0__schema.sql' and `checksum` <> 1275402472;
        update `zstack`.`schema_version` set `checksum`=-547730425  where `script`='V2.4.0.1__schema.sql' and `checksum` <> -547730425;
        update `zstack`.`schema_version` set `checksum`=1573104367  where `script`='V2.5.0__schema.sql' and `checksum` <> 1573104367;
        update `zstack`.`schema_version` set `checksum`=1564279419  where `script`='V3.0.0__schema.sql' and `checksum` <> 1564279419;
        update `zstack`.`schema_version` set `checksum`=271601676   where `script`='V3.1.0__schema.sql' and `checksum` <> 271601676;
        update `zstack`.`schema_version` set `checksum`=-1755080741 where `script`='V3.3.0__schema.sql' and `checksum` <> -1755080741;
        update `zstack`.`schema_version` set `checksum`=-624095644  where `script`='V3.5.2__schema.sql' and `checksum` <> -624095644;
        update `zstack`.`schema_version` set `checksum`=933644021   where `script`='V3.6.0__schema.sql' and `checksum` <> 933644021;
        update `zstack`.`schema_version` set `checksum`=565652311   where `script`='V3.7.2__schema.sql' and `checksum` <> 565652311;
        update `zstack`.`schema_version` set `checksum`=-143027462   where `script`='V3.9.1__schema.sql' and `checksum` <> -143027462;
    END IF;
END $$

DELIMITER  ;
CALL `zstack`.update_schema_checksum();
DROP PROCEDURE IF EXISTS `zstack`.update_schema_checksum;
