DROP PROCEDURE IF EXISTS `iam2_Organization_Quota`;
DELIMITER $$
CREATE PROCEDURE iam2_Organization_Quota()
BEGIN
    DECLARE iam2OrganizationUuid VARCHAR(32);
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR select uuid from IAM2OrganizationVO where rootOrganizationUuid = uuid and uuid <> '6e3d19dab98348d8bd67657378843f82';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO iam2OrganizationUuid;
        IF done THEN
            LEAVE read_loop;
        END IF;

        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='vm.cpuNum') THEN
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value) values (REPLACE(UUID(), '-', ''), 'vm.cpuNum', iam2OrganizationUuid, 'IAM2OrganizationVO', -1);
        END IF;
        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='portForwarding.num') THEN
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value) values (REPLACE(UUID(), '-', ''), 'portForwarding.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1);
        END IF;
        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='loadBalancer.num') THEN
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value) values (REPLACE(UUID(), '-', ''), 'loadBalancer.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1);
        END IF;
        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='l3.num') THEN
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value) values (REPLACE(UUID(), '-', ''), 'l3.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1);
        END IF;

        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='gpu.num') THEN
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value) values (REPLACE(UUID(), '-', ''), 'gpu.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1);
        END IF;
        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='sns.endpoint.num') THEN
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value) values (REPLACE(UUID(), '-', ''), 'sns.endpoint.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1);
        END IF;
        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='zwatch.alarm.num') THEN
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value) values (REPLACE(UUID(), '-', ''), 'zwatch.alarm.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1);
        END IF;
        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='vm.totalNum') THEN
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value) values (REPLACE(UUID(), '-', ''), 'vm.totalNum', iam2OrganizationUuid, 'IAM2OrganizationVO', -1);
        END IF;


        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='image.size') THEN
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value) values (REPLACE(UUID(), '-', ''), 'image.size', iam2OrganizationUuid, 'IAM2OrganizationVO', -1);
        END IF;
        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='scheduler.num') THEN
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value) values (REPLACE(UUID(), '-', ''), 'scheduler.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1);
        END IF;
        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='volume.backup.num') THEN
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value) values (REPLACE(UUID(), '-', ''), 'volume.backup.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1);
        END IF;
        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='eip.num') THEN
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value) values (REPLACE(UUID(), '-', ''), 'eip.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1);
        END IF;

        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='vm.memorySize') THEN
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value) values (REPLACE(UUID(), '-', ''), 'vm.memorySize', iam2OrganizationUuid, 'IAM2OrganizationVO', -1);
        END IF;
        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='vxlan.num') THEN
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value) values (REPLACE(UUID(), '-', ''), 'vxlan.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1);
        END IF;
        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='tag2.tag.num') THEN
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value) values (REPLACE(UUID(), '-', ''), 'tag2.tag.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1);
        END IF;
        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='snapshot.volume.num') THEN
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value) values (REPLACE(UUID(), '-', ''), 'snapshot.volume.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1);
        END IF;

        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='image.num') THEN
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value) values (REPLACE(UUID(), '-', ''), 'image.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1);
        END IF;
        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='vip.num') THEN
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value) values (REPLACE(UUID(), '-', ''), 'vip.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1);
        END IF;
        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='vm.num') THEN
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value) values (REPLACE(UUID(), '-', ''), 'vm.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1);
        END IF;
        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='zwatch.event.num') THEN
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value) values (REPLACE(UUID(), '-', ''), 'zwatch.event.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1);
        END IF;

        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='volume.capacity') THEN
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value) values (REPLACE(UUID(), '-', ''), 'volume.capacity', iam2OrganizationUuid, 'IAM2OrganizationVO', -1);
        END IF;
        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='volume.data.num') THEN
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value) values (REPLACE(UUID(), '-', ''), 'volume.data.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1);
        END IF;
        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='securityGroup.num') THEN
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value) values (REPLACE(UUID(), '-', ''), 'securityGroup.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1);
        END IF;
        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='volume.backup.size') THEN
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value) values (REPLACE(UUID(), '-', ''), 'volume.backup.size', iam2OrganizationUuid, 'IAM2OrganizationVO', -1);
        END IF;

        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='affinitygroup.num') THEN
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value) values (REPLACE(UUID(), '-', ''), 'affinitygroup.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1);
        END IF;
        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='listener.num') THEN
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value) values (REPLACE(UUID(), '-', ''), 'listener.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1);
        END IF;
        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='pci.num') THEN
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value) values (REPLACE(UUID(), '-', ''), 'pci.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1);
        END IF;
        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='scheduler.trigger.num') THEN
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value) values (REPLACE(UUID(), '-', ''), 'scheduler.trigger.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1);
        END IF;

        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='baremetal2.num') THEN
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value) values (REPLACE(UUID(), '-', ''), 'baremetal2.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1);
        END IF;

    END LOOP;
    CLOSE cur;
    SELECT CURTIME();
END $$
DELIMITER ;
CALL iam2_Organization_Quota();
DROP PROCEDURE iam2_Organization_Quota;