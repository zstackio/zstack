ALTER TABLE `QuotaVO` add index idxIdentityUuid (`identityUuid`);

DELIMITER $$
CREATE PROCEDURE addColumnsToSNSTextTemplateVO()
    BEGIN
        IF NOT EXISTS( SELECT 1
                       FROM INFORMATION_SCHEMA.COLUMNS
                       WHERE table_name = 'SNSTextTemplateVO'
                             AND table_schema = 'zstack'
                             AND column_name = 'subject') THEN

           ALTER TABLE `zstack`.`SNSTextTemplateVO` ADD COLUMN `subject` VARCHAR(2048);
        END IF;
        IF NOT EXISTS( SELECT 1
                       FROM INFORMATION_SCHEMA.COLUMNS
                       WHERE table_name = 'SNSTextTemplateVO'
                             AND table_schema = 'zstack'
                             AND column_name = 'recoverySubject') THEN

           ALTER TABLE `zstack`.`SNSTextTemplateVO` ADD COLUMN `recoverySubject` VARCHAR(2048);
        END IF;
    END $$
DELIMITER ;

call addColumnsToSNSTextTemplateVO();
DROP PROCEDURE IF EXISTS addColumnsToSNSTextTemplateVO;
