DROP PROCEDURE IF EXISTS `iam2_Organization_Quota`;
DELIMITER $$
CREATE PROCEDURE iam2_Organization_Quota()
BEGIN
    DECLARE iam2OrganizationUuid VARCHAR(32);
    DECLARE quotaUuid VARCHAR(32);
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
            set quotaUuid = REPLACE(UUID(), '-', '');
            INSERT INTO ResourceVO(uuid, resourceName, resourceType, concreteResourceType) values (quotaUuid, 'vm.cpuNum', 'QuotaVO', 'org.zstack.header.identity.QuotaVO');
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value, lastOpDate, createDate) values (quotaUuid, 'vm.cpuNum', iam2OrganizationUuid, 'IAM2OrganizationVO', -1, NOW(), NOW());
            INSERT INTO AccountResourceRefVO(`accountUuid`, `ownerAccountUuid`, `resourceUuid`, `resourceType`, `permission`, `isShared`, `lastOpDate`, `createDate`, `concreteResourceType`)
            values ('36c27e8ff05c4780bf6d2fa65700f22e', '36c27e8ff05c4780bf6d2fa65700f22e', quotaUuid, 'QuotaVO', 2, 0, NOW(), NOW(), 'org.zstack.header.identity.QuotaVO');
        END IF;
        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='portForwarding.num') THEN
            set quotaUuid = REPLACE(UUID(), '-', '');
            INSERT INTO ResourceVO(uuid, resourceName, resourceType, concreteResourceType) values (quotaUuid, 'portForwarding.num', 'QuotaVO', 'org.zstack.header.identity.QuotaVO');
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value, lastOpDate, createDate) values (quotaUuid, 'portForwarding.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1, NOW(), NOW());
            INSERT INTO AccountResourceRefVO(`accountUuid`, `ownerAccountUuid`, `resourceUuid`, `resourceType`, `permission`, `isShared`, `lastOpDate`, `createDate`, `concreteResourceType`)
            values ('36c27e8ff05c4780bf6d2fa65700f22e', '36c27e8ff05c4780bf6d2fa65700f22e', quotaUuid, 'QuotaVO', 2, 0, NOW(), NOW(), 'org.zstack.header.identity.QuotaVO');
        END IF;
        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='loadBalancer.num') THEN
            set quotaUuid = REPLACE(UUID(), '-', '');
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value, lastOpDate, createDate) values (quotaUuid, 'loadBalancer.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1, NOW(), NOW());
            INSERT INTO ResourceVO(uuid, resourceName, resourceType, concreteResourceType) values (quotaUuid, 'loadBalancer.num', 'QuotaVO', 'org.zstack.header.identity.QuotaVO');
            INSERT INTO AccountResourceRefVO(`accountUuid`, `ownerAccountUuid`, `resourceUuid`, `resourceType`, `permission`, `isShared`, `lastOpDate`, `createDate`, `concreteResourceType`)
            values ('36c27e8ff05c4780bf6d2fa65700f22e', '36c27e8ff05c4780bf6d2fa65700f22e', quotaUuid, 'QuotaVO', 2, 0, NOW(), NOW(), 'org.zstack.header.identity.QuotaVO');
        END IF;
        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='l3.num') THEN
            set quotaUuid = REPLACE(UUID(), '-', '');
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value, lastOpDate, createDate) values (quotaUuid, 'l3.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1, NOW(), NOW());
            INSERT INTO ResourceVO(uuid, resourceName, resourceType, concreteResourceType) values (quotaUuid, 'l3.num', 'QuotaVO', 'org.zstack.header.identity.QuotaVO');
            INSERT INTO AccountResourceRefVO(`accountUuid`, `ownerAccountUuid`, `resourceUuid`, `resourceType`, `permission`, `isShared`, `lastOpDate`, `createDate`, `concreteResourceType`)
            values ('36c27e8ff05c4780bf6d2fa65700f22e', '36c27e8ff05c4780bf6d2fa65700f22e', quotaUuid, 'QuotaVO', 2, 0, NOW(), NOW(), 'org.zstack.header.identity.QuotaVO');
        END IF;

        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='gpu.num') THEN
            set quotaUuid = REPLACE(UUID(), '-', '');
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value, lastOpDate, createDate) values (quotaUuid, 'gpu.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1, NOW(), NOW());
            INSERT INTO ResourceVO(uuid, resourceName, resourceType, concreteResourceType) values (quotaUuid, 'gpu.num', 'QuotaVO', 'org.zstack.header.identity.QuotaVO');
            INSERT INTO AccountResourceRefVO(`accountUuid`, `ownerAccountUuid`, `resourceUuid`, `resourceType`, `permission`, `isShared`, `lastOpDate`, `createDate`, `concreteResourceType`)
            values ('36c27e8ff05c4780bf6d2fa65700f22e', '36c27e8ff05c4780bf6d2fa65700f22e', quotaUuid, 'QuotaVO', 2, 0, NOW(), NOW(), 'org.zstack.header.identity.QuotaVO');
        END IF;
        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='sns.endpoint.num') THEN
            set quotaUuid = REPLACE(UUID(), '-', '');
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value, lastOpDate, createDate) values (quotaUuid, 'sns.endpoint.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1, NOW(), NOW());
            INSERT INTO ResourceVO(uuid, resourceName, resourceType, concreteResourceType) values (quotaUuid, 'sns.endpoint.num', 'QuotaVO', 'org.zstack.header.identity.QuotaVO');
            INSERT INTO AccountResourceRefVO(`accountUuid`, `ownerAccountUuid`, `resourceUuid`, `resourceType`, `permission`, `isShared`, `lastOpDate`, `createDate`, `concreteResourceType`)
            values ('36c27e8ff05c4780bf6d2fa65700f22e', '36c27e8ff05c4780bf6d2fa65700f22e', quotaUuid, 'QuotaVO', 2, 0, NOW(), NOW(), 'org.zstack.header.identity.QuotaVO');
        END IF;
        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='zwatch.alarm.num') THEN
            set quotaUuid = REPLACE(UUID(), '-', '');
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value, lastOpDate, createDate) values (quotaUuid, 'zwatch.alarm.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1, NOW(), NOW());
            INSERT INTO ResourceVO(uuid, resourceName, resourceType, concreteResourceType) values (quotaUuid, 'zwatch.alarm.num', 'QuotaVO', 'org.zstack.header.identity.QuotaVO');
            INSERT INTO AccountResourceRefVO(`accountUuid`, `ownerAccountUuid`, `resourceUuid`, `resourceType`, `permission`, `isShared`, `lastOpDate`, `createDate`, `concreteResourceType`)
            values ('36c27e8ff05c4780bf6d2fa65700f22e', '36c27e8ff05c4780bf6d2fa65700f22e', quotaUuid, 'QuotaVO', 2, 0, NOW(), NOW(), 'org.zstack.header.identity.QuotaVO');
        END IF;
        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='vm.totalNum') THEN
            set quotaUuid = REPLACE(UUID(), '-', '');
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value, lastOpDate, createDate) values (quotaUuid, 'vm.totalNum', iam2OrganizationUuid, 'IAM2OrganizationVO', -1, NOW(), NOW());
            INSERT INTO ResourceVO(uuid, resourceName, resourceType, concreteResourceType) values (quotaUuid, 'vm.totalNum', 'QuotaVO', 'org.zstack.header.identity.QuotaVO');
            INSERT INTO AccountResourceRefVO(`accountUuid`, `ownerAccountUuid`, `resourceUuid`, `resourceType`, `permission`, `isShared`, `lastOpDate`, `createDate`, `concreteResourceType`)
            values ('36c27e8ff05c4780bf6d2fa65700f22e', '36c27e8ff05c4780bf6d2fa65700f22e', quotaUuid, 'QuotaVO', 2, 0, NOW(), NOW(), 'org.zstack.header.identity.QuotaVO');
        END IF;


        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='image.size') THEN
            set quotaUuid = REPLACE(UUID(), '-', '');
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value, lastOpDate, createDate) values (quotaUuid, 'image.size', iam2OrganizationUuid, 'IAM2OrganizationVO', -1, NOW(), NOW());
            INSERT INTO ResourceVO(uuid, resourceName, resourceType, concreteResourceType) values (quotaUuid, 'image.size', 'QuotaVO', 'org.zstack.header.identity.QuotaVO');
            INSERT INTO AccountResourceRefVO(`accountUuid`, `ownerAccountUuid`, `resourceUuid`, `resourceType`, `permission`, `isShared`, `lastOpDate`, `createDate`, `concreteResourceType`)
            values ('36c27e8ff05c4780bf6d2fa65700f22e', '36c27e8ff05c4780bf6d2fa65700f22e', quotaUuid, 'QuotaVO', 2, 0, NOW(), NOW(), 'org.zstack.header.identity.QuotaVO');
        END IF;
        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='scheduler.num') THEN
            set quotaUuid = REPLACE(UUID(), '-', '');
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value, lastOpDate, createDate) values (quotaUuid, 'scheduler.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1, NOW(), NOW());
            INSERT INTO ResourceVO(uuid, resourceName, resourceType, concreteResourceType) values (quotaUuid, 'scheduler.num', 'QuotaVO', 'org.zstack.header.identity.QuotaVO');
            INSERT INTO AccountResourceRefVO(`accountUuid`, `ownerAccountUuid`, `resourceUuid`, `resourceType`, `permission`, `isShared`, `lastOpDate`, `createDate`, `concreteResourceType`)
            values ('36c27e8ff05c4780bf6d2fa65700f22e', '36c27e8ff05c4780bf6d2fa65700f22e', quotaUuid, 'QuotaVO', 2, 0, NOW(), NOW(), 'org.zstack.header.identity.QuotaVO');
        END IF;
        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='volume.backup.num') THEN
            set quotaUuid = REPLACE(UUID(), '-', '');
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value, lastOpDate, createDate) values (quotaUuid, 'volume.backup.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1, NOW(), NOW());
            INSERT INTO ResourceVO(uuid, resourceName, resourceType, concreteResourceType) values (quotaUuid, 'volume.backup.num', 'QuotaVO', 'org.zstack.header.identity.QuotaVO');
            INSERT INTO AccountResourceRefVO(`accountUuid`, `ownerAccountUuid`, `resourceUuid`, `resourceType`, `permission`, `isShared`, `lastOpDate`, `createDate`, `concreteResourceType`)
            values ('36c27e8ff05c4780bf6d2fa65700f22e', '36c27e8ff05c4780bf6d2fa65700f22e', quotaUuid, 'QuotaVO', 2, 0, NOW(), NOW(), 'org.zstack.header.identity.QuotaVO');
        END IF;
        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='eip.num') THEN
            set quotaUuid = REPLACE(UUID(), '-', '');
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value, lastOpDate, createDate) values (quotaUuid, 'eip.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1, NOW(), NOW());
            INSERT INTO ResourceVO(uuid, resourceName, resourceType, concreteResourceType) values (quotaUuid, 'eip.num', 'QuotaVO', 'org.zstack.header.identity.QuotaVO');
            INSERT INTO AccountResourceRefVO(`accountUuid`, `ownerAccountUuid`, `resourceUuid`, `resourceType`, `permission`, `isShared`, `lastOpDate`, `createDate`, `concreteResourceType`)
            values ('36c27e8ff05c4780bf6d2fa65700f22e', '36c27e8ff05c4780bf6d2fa65700f22e', quotaUuid, 'QuotaVO', 2, 0, NOW(), NOW(), 'org.zstack.header.identity.QuotaVO');
        END IF;

        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='vm.memorySize') THEN
            set quotaUuid = REPLACE(UUID(), '-', '');
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value, lastOpDate, createDate) values (quotaUuid, 'vm.memorySize', iam2OrganizationUuid, 'IAM2OrganizationVO', -1, NOW(), NOW());
            INSERT INTO ResourceVO(uuid, resourceName, resourceType, concreteResourceType) values (quotaUuid, 'vm.memorySize', 'QuotaVO', 'org.zstack.header.identity.QuotaVO');
            INSERT INTO AccountResourceRefVO(`accountUuid`, `ownerAccountUuid`, `resourceUuid`, `resourceType`, `permission`, `isShared`, `lastOpDate`, `createDate`, `concreteResourceType`)
            values ('36c27e8ff05c4780bf6d2fa65700f22e', '36c27e8ff05c4780bf6d2fa65700f22e', quotaUuid, 'QuotaVO', 2, 0, NOW(), NOW(), 'org.zstack.header.identity.QuotaVO');
        END IF;
        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='vxlan.num') THEN
            set quotaUuid = REPLACE(UUID(), '-', '');
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value, lastOpDate, createDate) values (quotaUuid, 'vxlan.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1, NOW(), NOW());
            INSERT INTO ResourceVO(uuid, resourceName, resourceType, concreteResourceType) values (quotaUuid, 'vxlan.num', 'QuotaVO', 'org.zstack.header.identity.QuotaVO');
            INSERT INTO AccountResourceRefVO(`accountUuid`, `ownerAccountUuid`, `resourceUuid`, `resourceType`, `permission`, `isShared`, `lastOpDate`, `createDate`, `concreteResourceType`)
            values ('36c27e8ff05c4780bf6d2fa65700f22e', '36c27e8ff05c4780bf6d2fa65700f22e', quotaUuid, 'QuotaVO', 2, 0, NOW(), NOW(), 'org.zstack.header.identity.QuotaVO');
        END IF;
        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='tag2.tag.num') THEN
            set quotaUuid = REPLACE(UUID(), '-', '');
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value, lastOpDate, createDate) values (quotaUuid, 'tag2.tag.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1, NOW(), NOW());
            INSERT INTO ResourceVO(uuid, resourceName, resourceType, concreteResourceType) values (quotaUuid, 'tag2.tag.num', 'QuotaVO', 'org.zstack.header.identity.QuotaVO');
            INSERT INTO AccountResourceRefVO(`accountUuid`, `ownerAccountUuid`, `resourceUuid`, `resourceType`, `permission`, `isShared`, `lastOpDate`, `createDate`, `concreteResourceType`)
            values ('36c27e8ff05c4780bf6d2fa65700f22e', '36c27e8ff05c4780bf6d2fa65700f22e', quotaUuid, 'QuotaVO', 2, 0, NOW(), NOW(), 'org.zstack.header.identity.QuotaVO');
        END IF;
        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='snapshot.volume.num') THEN
            set quotaUuid = REPLACE(UUID(), '-', '');
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value, lastOpDate, createDate) values (quotaUuid, 'snapshot.volume.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1, NOW(), NOW());
            INSERT INTO ResourceVO(uuid, resourceName, resourceType, concreteResourceType) values (quotaUuid, 'snapshot.volume.num', 'QuotaVO', 'org.zstack.header.identity.QuotaVO');
            INSERT INTO AccountResourceRefVO(`accountUuid`, `ownerAccountUuid`, `resourceUuid`, `resourceType`, `permission`, `isShared`, `lastOpDate`, `createDate`, `concreteResourceType`)
            values ('36c27e8ff05c4780bf6d2fa65700f22e', '36c27e8ff05c4780bf6d2fa65700f22e', quotaUuid, 'QuotaVO', 2, 0, NOW(), NOW(), 'org.zstack.header.identity.QuotaVO');
        END IF;

        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='image.num') THEN
            set quotaUuid = REPLACE(UUID(), '-', '');
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value, lastOpDate, createDate) values (quotaUuid, 'image.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1, NOW(), NOW());
            INSERT INTO ResourceVO(uuid, resourceName, resourceType, concreteResourceType) values (quotaUuid, 'image.num', 'QuotaVO', 'org.zstack.header.identity.QuotaVO');
            INSERT INTO AccountResourceRefVO(`accountUuid`, `ownerAccountUuid`, `resourceUuid`, `resourceType`, `permission`, `isShared`, `lastOpDate`, `createDate`, `concreteResourceType`)
            values ('36c27e8ff05c4780bf6d2fa65700f22e', '36c27e8ff05c4780bf6d2fa65700f22e', quotaUuid, 'QuotaVO', 2, 0, NOW(), NOW(), 'org.zstack.header.identity.QuotaVO');
        END IF;
        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='vip.num') THEN
            set quotaUuid = REPLACE(UUID(), '-', '');
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value, lastOpDate, createDate) values (quotaUuid, 'vip.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1, NOW(), NOW());
            INSERT INTO ResourceVO(uuid, resourceName, resourceType, concreteResourceType) values (quotaUuid, 'vip.num', 'QuotaVO', 'org.zstack.header.identity.QuotaVO');
            INSERT INTO AccountResourceRefVO(`accountUuid`, `ownerAccountUuid`, `resourceUuid`, `resourceType`, `permission`, `isShared`, `lastOpDate`, `createDate`, `concreteResourceType`)
            values ('36c27e8ff05c4780bf6d2fa65700f22e', '36c27e8ff05c4780bf6d2fa65700f22e', quotaUuid, 'QuotaVO', 2, 0, NOW(), NOW(), 'org.zstack.header.identity.QuotaVO');
        END IF;
        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='vm.num') THEN
            set quotaUuid = REPLACE(UUID(), '-', '');
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value, lastOpDate, createDate) values (quotaUuid, 'vm.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1, NOW(), NOW());
            INSERT INTO ResourceVO(uuid, resourceName, resourceType, concreteResourceType) values (quotaUuid, 'vm.num', 'QuotaVO', 'org.zstack.header.identity.QuotaVO');
            INSERT INTO AccountResourceRefVO(`accountUuid`, `ownerAccountUuid`, `resourceUuid`, `resourceType`, `permission`, `isShared`, `lastOpDate`, `createDate`, `concreteResourceType`)
            values ('36c27e8ff05c4780bf6d2fa65700f22e', '36c27e8ff05c4780bf6d2fa65700f22e', quotaUuid, 'QuotaVO', 2, 0, NOW(), NOW(), 'org.zstack.header.identity.QuotaVO');
        END IF;
        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='zwatch.event.num') THEN
            set quotaUuid = REPLACE(UUID(), '-', '');
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value, lastOpDate, createDate) values (quotaUuid, 'zwatch.event.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1, NOW(), NOW());
            INSERT INTO ResourceVO(uuid, resourceName, resourceType, concreteResourceType) values (quotaUuid, 'zwatch.event.num', 'QuotaVO', 'org.zstack.header.identity.QuotaVO');
            INSERT INTO AccountResourceRefVO(`accountUuid`, `ownerAccountUuid`, `resourceUuid`, `resourceType`, `permission`, `isShared`, `lastOpDate`, `createDate`, `concreteResourceType`)
            values ('36c27e8ff05c4780bf6d2fa65700f22e', '36c27e8ff05c4780bf6d2fa65700f22e', quotaUuid, 'QuotaVO', 2, 0, NOW(), NOW(), 'org.zstack.header.identity.QuotaVO');
        END IF;

        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='volume.capacity') THEN
            set quotaUuid = REPLACE(UUID(), '-', '');
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value, lastOpDate, createDate) values (quotaUuid, 'volume.capacity', iam2OrganizationUuid, 'IAM2OrganizationVO', -1, NOW(), NOW());
            INSERT INTO ResourceVO(uuid, resourceName, resourceType, concreteResourceType) values (quotaUuid, 'volume.capacity', 'QuotaVO', 'org.zstack.header.identity.QuotaVO');
            INSERT INTO AccountResourceRefVO(`accountUuid`, `ownerAccountUuid`, `resourceUuid`, `resourceType`, `permission`, `isShared`, `lastOpDate`, `createDate`, `concreteResourceType`)
            values ('36c27e8ff05c4780bf6d2fa65700f22e', '36c27e8ff05c4780bf6d2fa65700f22e', quotaUuid, 'QuotaVO', 2, 0, NOW(), NOW(), 'org.zstack.header.identity.QuotaVO');
        END IF;
        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='volume.data.num') THEN
            set quotaUuid = REPLACE(UUID(), '-', '');
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value, lastOpDate, createDate) values (quotaUuid, 'volume.data.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1, NOW(), NOW());
            INSERT INTO ResourceVO(uuid, resourceName, resourceType, concreteResourceType) values (quotaUuid, 'volume.data.num', 'QuotaVO', 'org.zstack.header.identity.QuotaVO');
            INSERT INTO AccountResourceRefVO(`accountUuid`, `ownerAccountUuid`, `resourceUuid`, `resourceType`, `permission`, `isShared`, `lastOpDate`, `createDate`, `concreteResourceType`)
            values ('36c27e8ff05c4780bf6d2fa65700f22e', '36c27e8ff05c4780bf6d2fa65700f22e', quotaUuid, 'QuotaVO', 2, 0, NOW(), NOW(), 'org.zstack.header.identity.QuotaVO');
        END IF;
        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='securityGroup.num') THEN
            set quotaUuid = REPLACE(UUID(), '-', '');
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value, lastOpDate, createDate) values (quotaUuid, 'securityGroup.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1, NOW(), NOW());
            INSERT INTO ResourceVO(uuid, resourceName, resourceType, concreteResourceType) values (quotaUuid, 'securityGroup.num', 'QuotaVO', 'org.zstack.header.identity.QuotaVO');
            INSERT INTO AccountResourceRefVO(`accountUuid`, `ownerAccountUuid`, `resourceUuid`, `resourceType`, `permission`, `isShared`, `lastOpDate`, `createDate`, `concreteResourceType`)
            values ('36c27e8ff05c4780bf6d2fa65700f22e', '36c27e8ff05c4780bf6d2fa65700f22e', quotaUuid, 'QuotaVO', 2, 0, NOW(), NOW(), 'org.zstack.header.identity.QuotaVO');
        END IF;
        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='volume.backup.size') THEN
            set quotaUuid = REPLACE(UUID(), '-', '');
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value, lastOpDate, createDate) values (quotaUuid, 'volume.backup.size', iam2OrganizationUuid, 'IAM2OrganizationVO', -1, NOW(), NOW());
            INSERT INTO ResourceVO(uuid, resourceName, resourceType, concreteResourceType) values (quotaUuid, 'volume.backup.size', 'QuotaVO', 'org.zstack.header.identity.QuotaVO');
            INSERT INTO AccountResourceRefVO(`accountUuid`, `ownerAccountUuid`, `resourceUuid`, `resourceType`, `permission`, `isShared`, `lastOpDate`, `createDate`, `concreteResourceType`)
            values ('36c27e8ff05c4780bf6d2fa65700f22e', '36c27e8ff05c4780bf6d2fa65700f22e', quotaUuid, 'QuotaVO', 2, 0, NOW(), NOW(), 'org.zstack.header.identity.QuotaVO');
        END IF;

        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='affinitygroup.num') THEN
            set quotaUuid = REPLACE(UUID(), '-', '');
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value, lastOpDate, createDate) values (quotaUuid, 'affinitygroup.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1, NOW(), NOW());
            INSERT INTO ResourceVO(uuid, resourceName, resourceType, concreteResourceType) values (quotaUuid, 'affinitygroup.num', 'QuotaVO', 'org.zstack.header.identity.QuotaVO');
            INSERT INTO AccountResourceRefVO(`accountUuid`, `ownerAccountUuid`, `resourceUuid`, `resourceType`, `permission`, `isShared`, `lastOpDate`, `createDate`, `concreteResourceType`)
            values ('36c27e8ff05c4780bf6d2fa65700f22e', '36c27e8ff05c4780bf6d2fa65700f22e', quotaUuid, 'QuotaVO', 2, 0, NOW(), NOW(), 'org.zstack.header.identity.QuotaVO');
        END IF;
        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='listener.num') THEN
            set quotaUuid = REPLACE(UUID(), '-', '');
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value, lastOpDate, createDate) values (quotaUuid, 'listener.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1, NOW(), NOW());
            INSERT INTO ResourceVO(uuid, resourceName, resourceType, concreteResourceType) values (quotaUuid, 'listener.num', 'QuotaVO', 'org.zstack.header.identity.QuotaVO');
            INSERT INTO AccountResourceRefVO(`accountUuid`, `ownerAccountUuid`, `resourceUuid`, `resourceType`, `permission`, `isShared`, `lastOpDate`, `createDate`, `concreteResourceType`)
            values ('36c27e8ff05c4780bf6d2fa65700f22e', '36c27e8ff05c4780bf6d2fa65700f22e', quotaUuid, 'QuotaVO', 2, 0, NOW(), NOW(), 'org.zstack.header.identity.QuotaVO');
        END IF;
        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='pci.num') THEN
            set quotaUuid = REPLACE(UUID(), '-', '');
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value, lastOpDate, createDate) values (quotaUuid, 'pci.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1, NOW(), NOW());
            INSERT INTO ResourceVO(uuid, resourceName, resourceType, concreteResourceType) values (quotaUuid, 'pci.num', 'QuotaVO', 'org.zstack.header.identity.QuotaVO');
            INSERT INTO AccountResourceRefVO(`accountUuid`, `ownerAccountUuid`, `resourceUuid`, `resourceType`, `permission`, `isShared`, `lastOpDate`, `createDate`, `concreteResourceType`)
            values ('36c27e8ff05c4780bf6d2fa65700f22e', '36c27e8ff05c4780bf6d2fa65700f22e', quotaUuid, 'QuotaVO', 2, 0, NOW(), NOW(), 'org.zstack.header.identity.QuotaVO');
        END IF;
        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='scheduler.trigger.num') THEN
            set quotaUuid = REPLACE(UUID(), '-', '');
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value, lastOpDate, createDate) values (quotaUuid, 'scheduler.trigger.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1, NOW(), NOW());
            INSERT INTO ResourceVO(uuid, resourceName, resourceType, concreteResourceType) values (quotaUuid, 'scheduler.trigger.num', 'QuotaVO', 'org.zstack.header.identity.QuotaVO');
            INSERT INTO AccountResourceRefVO(`accountUuid`, `ownerAccountUuid`, `resourceUuid`, `resourceType`, `permission`, `isShared`, `lastOpDate`, `createDate`, `concreteResourceType`)
            values ('36c27e8ff05c4780bf6d2fa65700f22e', '36c27e8ff05c4780bf6d2fa65700f22e', quotaUuid, 'QuotaVO', 2, 0, NOW(), NOW(), 'org.zstack.header.identity.QuotaVO');
        END IF;

        IF NOT EXISTS (select NULL from QuotaVO where identityType = 'IAM2OrganizationVO' and identityUuid = iam2OrganizationUuid and name ='baremetal2.num') THEN
            set quotaUuid = REPLACE(UUID(), '-', '');
            INSERT INTO QuotaVO(uuid, name, identityUuid, identityType, value, lastOpDate, createDate) values (quotaUuid, 'baremetal2.num', iam2OrganizationUuid, 'IAM2OrganizationVO', -1, NOW(), NOW());
            INSERT INTO ResourceVO(uuid, resourceName, resourceType, concreteResourceType) values (quotaUuid, 'baremetal2.num', 'QuotaVO', 'org.zstack.header.identity.QuotaVO');
            INSERT INTO AccountResourceRefVO(`accountUuid`, `ownerAccountUuid`, `resourceUuid`, `resourceType`, `permission`, `isShared`, `lastOpDate`, `createDate`, `concreteResourceType`)
            values ('36c27e8ff05c4780bf6d2fa65700f22e', '36c27e8ff05c4780bf6d2fa65700f22e', quotaUuid, 'QuotaVO', 2, 0, NOW(), NOW(), 'org.zstack.header.identity.QuotaVO');
        END IF;

    END LOOP;
    CLOSE cur;
    SELECT CURTIME();
END $$
DELIMITER ;
CALL iam2_Organization_Quota();
DROP PROCEDURE iam2_Organization_Quota;

CREATE TABLE IF NOT EXISTS  `zstack`.`VpcSnatStateVO` (
                                                          `uuid` varchar(32) NOT NULL,
                                                          `vpcUuid` varchar(32) NOT NULL,
                                                          `l3NetworkUuid` varchar(32) NOT NULL,
                                                          `state` varchar(32) NOT NULL,
                                                          `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
                                                          `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
                                                          PRIMARY KEY (`uuid`),
                                                          UNIQUE KEY `uuid` (`uuid`) USING BTREE,
                                                          CONSTRAINT fkVpcNetworkServiceRefVOVirtualRouterVmVO FOREIGN KEY (vpcUuid) REFERENCES VirtualRouterVmVO (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP PROCEDURE IF EXISTS addSnatConfigForVirtualRouter;
DELIMITER $$
CREATE PROCEDURE addSnatConfigForVirtualRouter()
BEGIN
    DECLARE virtualRouterUuid VARCHAR(32);
    DECLARE publicNetworkUuid VARCHAR(32);
    DECLARE ruuid VARCHAR(32);
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR SELECT vrv.uuid, vrv.publicNetworkUuid FROM `zstack`.`VirtualRouterVmVO` vrv WHERE vrv.uuid IN (SELECT uuid FROM `zstack`.`ApplianceVmVO` WHERE `haStatus` = 'NoHa');
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO virtualRouterUuid, publicNetworkUuid;
        IF done THEN
            LEAVE read_loop;
        END IF;
        SET ruuid = REPLACE(UUID(), '-', '');
        INSERT INTO `zstack`.`VpcSnatStateVO` (uuid, vpcUuid, l3NetworkUuid, state, createDate, lastOpDate)
        values(ruuid, virtualRouterUuid, publicNetworkUuid, 'enable', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
    END LOOP;
    CLOSE cur;
    DELETE FROM `zstack`.`VpcSnatStateVO` WHERE vpcUuid IN (
        SELECT resourceUuid FROM SystemTagVO WHERE tag = 'disabledService::SNAT'
    );
    SELECT CURTIME();
END $$
DELIMITER ;
CALL addSnatConfigForVirtualRouter();
DROP PROCEDURE IF EXISTS addSnatConfigForVirtualRouter;

DROP PROCEDURE IF EXISTS addSnatConfigForVpcHa;
DELIMITER $$
CREATE PROCEDURE addSnatConfigForVpcHa()
BEGIN
    DECLARE thisVpcHaRouterUuid VARCHAR(32);
    DECLARE vpcHaRouterDefaultPublicNetworkUuid VARCHAR(32);
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR SELECT DISTINCT vgr.vpcHaRouterUuid FROM `zstack`.`VpcHaGroupApplianceVmRefVO` vgr ;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    OPEN cur;
    DELETE FROM `zstack`.`VpcHaGroupNetworkServiceRefVO` WHERE networkServiceName = 'SNAT' AND networkServiceUuid != 'true';
    read_loop: LOOP
        FETCH cur INTO thisVpcHaRouterUuid;
        IF done THEN
            LEAVE read_loop;
        END IF;

        SELECT publicNetworkUuid
        FROM `zstack`.`VirtualRouterVmVO`
        WHERE uuid IN (SELECT uuid
                       FROM `zstack`.`ApplianceVmVO`
                       WHERE uuid IN (SELECT uuid
                                      FROM `zstack`.`VpcHaGroupApplianceVmRefVO`
                                      WHERE `vpcHaRouterUuid` = thisVpcHaRouterUuid)
                         AND `haStatus` != 'NoHa')
        LIMIT 1
        INTO vpcHaRouterDefaultPublicNetworkUuid;

        INSERT INTO `zstack`.`VpcHaGroupNetworkServiceRefVO` (vpcHaRouterUuid, networkServiceName, networkServiceUuid, lastOpDate, createDate)
        values(thisVpcHaRouterUuid, 'SNAT', vpcHaRouterDefaultPublicNetworkUuid, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
    END LOOP;
    CLOSE cur;
    DELETE FROM `zstack`.`VpcHaGroupNetworkServiceRefVO` WHERE networkServiceName = 'SNAT' AND vpcHaRouterUuid IN
                                                                                               (SELECT a.vpcHaRouterUuid FROM
                                                                                                   (SELECT vpcHaRouterUuid FROM `zstack`.`VpcHaGroupNetworkServiceRefVO` WHERE networkServiceName = 'SNAT' AND networkServiceUuid = 'true') a);
    SELECT CURTIME();
END $$
DELIMITER ;
CALL addSnatConfigForVpcHa();
DROP PROCEDURE IF EXISTS addSnatConfigForVpcHa;

ALTER TABLE `BareMetal2ChassisVO` ADD COLUMN `provisionType` varchar(32) NOT NULL DEFAULT 'Remote';
ALTER TABLE `BareMetal2InstanceVO` ADD COLUMN `provisionType` varchar(32) NOT NULL DEFAULT 'Remote';
ALTER TABLE `BareMetal2ChassisOfferingVO` ADD COLUMN `provisionType` varchar(32) NOT NULL DEFAULT 'Remote';
ALTER TABLE `BareMetal2ChassisDiskVO` ADD COLUMN `wwn` varchar(128) DEFAULT NULL;
