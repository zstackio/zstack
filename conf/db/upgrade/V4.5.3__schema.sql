alter table LicenseHistoryVO modify COLUMN `userName` varchar(64) NOT NULL;

ALTER TABLE LicenseHistoryVO ADD COLUMN capacity int(10) NOT NULL;

CREATE TABLE  IF NOT EXISTS `zstack`.`IAM2VirtualIDInformationVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `phone` varchar(255),
    `mail` varchar(255),
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkIAM2VirtualIDInformationVOIAM2VirtualIDVO` FOREIGN KEY (`uuid`) REFERENCES `IAM2VirtualIDVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DELIMITER $$
CREATE PROCEDURE attributePhoneToInformation()
    BEGIN
        DECLARE vitualIdPhone VARCHAR(32);
        DECLARE vitualIdUuid VARCHAR(32);
        DECLARE done INT DEFAULT FALSE;
        DECLARE phoneCursor CURSOR FOR SELECT virtualIDUuid, value from `zstack`.`IAM2VirtualIDAttributeVO` WHERE name = "phone";
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

        open phoneCursor;
        read_loop: LOOP
            FETCH phoneCursor INTO vitualIdUuid, vitualIdPhone;
            IF done THEN
                LEAVE read_loop;
            END IF;

            IF (select count(*) from IAM2VirtualIDInformationVO where uuid = vitualIdUuid) = 0 THEN
                INSERT `zstack`.`IAM2VirtualIDInformationVO`(uuid, phone) values (vitualIdUuid, vitualIdPhone);
            else
                update `zstack`.`IAM2VirtualIDInformationVO` set phone = vitualIdPhone where uuid = vitualIdUuid;
            END IF;

        END LOOP;
        close phoneCursor;
        SELECT CURTIME();
    END $$
DELIMITER ;

call attributePhoneToInformation();
DROP PROCEDURE IF EXISTS attributePhoneToInformation;

DELIMITER $$
CREATE PROCEDURE attributeMailToInformation()
    BEGIN
        DECLARE vitualIdMail VARCHAR(32);
        DECLARE vitualIdUuid VARCHAR(32);
        DECLARE done INT DEFAULT FALSE;
        DECLARE mailCursor CURSOR FOR SELECT virtualIDUuid, value from `zstack`.`IAM2VirtualIDAttributeVO` WHERE name = "mail";
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

        open mailCursor;
        read_loop: LOOP
            FETCH mailCursor INTO vitualIdUuid, vitualIdMail;
            IF done THEN
                LEAVE read_loop;
            END IF;

            IF (select count(*) from IAM2VirtualIDInformationVO where uuid = vitualIdUuid) = 0 THEN
                INSERT `zstack`.`IAM2VirtualIDInformationVO`(uuid, mail) values (vitualIdUuid, vitualIdMail);
            ELSE
                update `zstack`.`IAM2VirtualIDInformationVO` set mail = vitualIdMail where uuid = vitualIdUuid;
            END IF;

        END LOOP;
        close mailCursor;
        SELECT CURTIME();
    END $$
DELIMITER ;

call attributeMailToInformation();
DROP PROCEDURE IF EXISTS attributeMailToInformation;
