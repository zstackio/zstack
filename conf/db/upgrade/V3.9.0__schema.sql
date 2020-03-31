ALTER TABLE JsonLabelVO MODIFY COLUMN labelValue MEDIUMTEXT;

CREATE INDEX idxTaskProgressVOapiId ON TaskProgressVO(apiId);

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE DahoVllVbrRefVO;
DROP TABLE DahoCloudConnectionVO;
DROP TABLE DahoVllsVO;
DROP TABLE DahoConnectionVO;
DROP TABLE DahoDCAccessVO;

SET FOREIGN_KEY_CHECKS = 1;

ALTER TABLE ImageBackupStorageRefVO ADD COLUMN exportMd5Sum VARCHAR(255) DEFAULT NULL;
ALTER TABLE ImageBackupStorageRefVO ADD COLUMN exportUrl VARCHAR(2048) DEFAULT NULL;
UPDATE ImageBackupStorageRefVO ibs, ImageVO i SET ibs.exportMd5Sum = i.exportMd5Sum, ibs.exportUrl = i.exportUrl WHERE ibs.imageUuid = i.uuid;
DROP VIEW IF EXISTS `zstack`.`ImageVO`;
CREATE VIEW `zstack`.`ImageVO` AS SELECT uuid, name, description, status, state, size, actualSize, md5Sum, platform, type, format, url, system, mediaType, createDate, lastOpDate, guestOsType FROM `zstack`.`ImageEO` WHERE deleted IS NULL;
ALTER TABLE ImageEO DROP exportMd5Sum, DROP exportUrl;

ALTER TABLE `zstack`.`PolicyRouteRuleSetVO` ADD COLUMN type VARCHAR(64) DEFAULT "User" NOT NULL;
ALTER TABLE `zstack`.`PolicyRouteTableVO` ADD COLUMN type VARCHAR(64) DEFAULT "User" NOT NULL;

CREATE TABLE  `zstack`.`NormalIpRangeVO` (
                                             `uuid` varchar(32) NOT NULL UNIQUE,
                                             PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
ALTER TABLE `zstack`.`NormalIpRangeVO` ADD CONSTRAINT fkNormalIpRangeVOIpRangeEO FOREIGN KEY (uuid) REFERENCES `zstack`.`IpRangeEO` (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;

CREATE TABLE  `zstack`.`AddressPoolVO` (
                                             `uuid` varchar(32) NOT NULL UNIQUE,
                                             PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
ALTER TABLE `zstack`.`AddressPoolVO` ADD CONSTRAINT fkAddressPoolVOIpRangeEO FOREIGN KEY (uuid) REFERENCES `zstack`.`IpRangeEO` (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;

DELIMITER $$
CREATE PROCEDURE generateNormalpRangeVO()
BEGIN
    DECLARE ipRangeUuid VARCHAR(32);
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR SELECT uuid FROM `zstack`.`IpRangeVO`;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO ipRangeUuid;
        IF done THEN
            LEAVE read_loop;
        END IF;

        INSERT INTO zstack.NormalIpRangeVO (uuid) values(ipRangeUuid);

    END LOOP;
    CLOSE cur;
END $$
DELIMITER ;

CALL generateNormalpRangeVO();
DROP PROCEDURE IF EXISTS generateNormalpRangeVO;