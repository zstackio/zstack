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

CREATE TABLE IF NOT EXISTS `zstack`.`VpcFirewallIpSetTemplateVO`
(
    `uuid`        varchar(32)  NOT NULL,
    `name`        varchar(255) NOT NULL,
    `sourceValue` varchar(2048)         DEFAULT NULL,
    `destValue`   varchar(2048)         DEFAULT NULL,
    `type`        varchar(255) NOT NULL,
    `createDate`  timestamp    NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate`  timestamp    NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uuid` (`uuid`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`VpcFirewallRuleTemplateVO`
(
    `uuid`         varchar(32)  NOT NULL,
    `action`       varchar(255) NOT NULL,
    `name`         varchar(255) NOT NULL,
    `protocol`     varchar(255)          DEFAULT NULL,
    `sourcePort`   varchar(255)          DEFAULT NULL,
    `destPort`     varchar(255)          DEFAULT NULL,
    `sourceIp`     varchar(2048)         DEFAULT NULL,
    `destIp`       varchar(2048)         DEFAULT NULL,
    `ruleNumber`   int(10)      NOT NULL,
    `icmpTypeName` varchar(255)          DEFAULT NULL,
    `allowStates`  varchar(255)          DEFAULT NULL,
    `tcpFlag`      varchar(255)          DEFAULT NULL,
    `enableLog`    tinyint(1)   NOT NULL DEFAULT '0',
    `state`        varchar(32)  NOT NULL DEFAULT '0',
    `isDefault`    tinyint(1)   NOT NULL DEFAULT '0',
    `description`  varchar(2048)         DEFAULT NULL,
    `createDate`   timestamp    NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate`   timestamp    NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uuid` (`uuid`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

ALTER TABLE `zstack`.`VpcFirewallRuleSetVO` ADD COLUMN `isApplied` boolean NOT NULL DEFAULT TRUE;
ALTER TABLE `zstack`.`VpcFirewallRuleSetVO` MODIFY `actionType` varchar(255) DEFAULT NULL;
ALTER TABLE `zstack`.`VpcFirewallRuleVO` ADD COLUMN `isApplied` boolean NOT NULL DEFAULT TRUE;
ALTER TABLE `zstack`.`VpcFirewallRuleVO` ADD COLUMN `expired` boolean NOT NULL DEFAULT FALSE;

ALTER TABLE `zstack`.`VpcFirewallRuleSetVO` DROP FOREIGN KEY fkVpcFirewallRuleSetVOVpcFirewallVO;
ALTER TABLE `zstack`.`VpcFirewallRuleVO` DROP FOREIGN KEY fkVpcFirewallRuleVOVpcFirewallVO;
ALTER TABLE `zstack`.`VpcFirewallRuleVO` DROP FOREIGN KEY fkVpcFirewallRuleVOVpcFirewallRuleSetVO;
ALTER TABLE `VpcFirewallRuleSetVO` DROP INDEX `fkVpcFirewallRuleSetVOVpcFirewallVO`;
ALTER TABLE `VpcFirewallRuleVO` DROP INDEX `fkVpcFirewallRuleVOVpcFirewallVO`;
ALTER TABLE `VpcFirewallRuleVO` DROP INDEX `fkVpcFirewallRuleVOVpcFirewallRuleSetVO`;
ALTER TABLE `zstack`.`VpcFirewallRuleSetVO` DROP COLUMN `vyosName`;
ALTER TABLE `zstack`.`VpcFirewallRuleSetVO` DROP COLUMN `vpcFirewallUuid`;
ALTER TABLE `zstack`.`VpcFirewallRuleVO` DROP COLUMN `vpcFirewallUuid`;
ALTER TABLE `zstack`.`VpcFirewallRuleVO` DROP COLUMN `ruleSetName`;

CREATE TABLE `IAM2ProjectRoleVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `iam2ProjectRoleType` VARCHAR(64) NOT NULL,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE IAM2VirtualIDRoleRefVO DROP FOREIGN KEY fkIAM2VirtualIDRoleRefVOIAM2VirtualIDVO;
ALTER TABLE IAM2VirtualIDRoleRefVO DROP FOREIGN KEY fkIAM2VirtualIDRoleRefVORoleVO;
ALTER TABLE IAM2VirtualIDRoleRefVO ADD COLUMN `targetAccountUuid` varchar(32) NOT NULL;
ALTER TABLE IAM2VirtualIDRoleRefVO DROP PRIMARY KEY, ADD PRIMARY KEY(virtualIDUuid, roleUuid, targetAccountUuid);
ALTER TABLE IAM2VirtualIDRoleRefVO ADD CONSTRAINT fkIAM2VirtualIDRoleRefVOIAM2VirtualIDVO FOREIGN KEY (virtualIDUuid) REFERENCES IAM2VirtualIDVO (uuid) ON DELETE CASCADE;
ALTER TABLE IAM2VirtualIDRoleRefVO ADD CONSTRAINT fkIAM2VirtualIDRoleRefVORoleVO FOREIGN KEY (roleUuid) REFERENCES RoleVO (uuid) ON DELETE CASCADE;
CREATE INDEX idxIAM2VirtualIDRoleRefVOTargetAccountUuid ON IAM2VirtualIDRoleRefVO (targetAccountUuid);

# upgrade PROJECT_OPERATOR_OF_PROJECT and PROJECT_ADMIN_OF_PROJECT to new data structure
DROP PROCEDURE IF EXISTS upgradeProjectOperatorSystemTags;
DELIMITER $$
CREATE PROCEDURE upgradeProjectOperatorSystemTags()
BEGIN
    DECLARE projectOperatorTag VARCHAR(62);
    DECLARE targetProjectUuid VARCHAR(32);
    DECLARE targetAccountUuid VARCHAR(32);
    DECLARE iam2VirtualIDUuid VARCHAR(32);
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR SELECT systemTag.tag, systemTag.resourceUuid FROM `zstack`.`SystemTagVO` systemTag where systemTag.tag like 'projectOperatorOfProjectUuid::%';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO projectOperatorTag, iam2VirtualIDUuid;
        IF done THEN
            LEAVE read_loop;
        END IF;

        SET targetProjectUuid = SUBSTRING_INDEX(projectOperatorTag, '::', 1);
        SELECT `accountUuid` into targetAccountUuid FROM `IAM2ProjectAccountRefVO` WHERE `projectUuid` = targetProjectUuid LIMIT 1;

        INSERT INTO IAM2VirtualIDRoleRefVO (`virtualIDUuid`, `roleUuid`, `targetAccountUuid`) VALUES (iam2VirtualIDUuid, 'f2f474c60e7340c0a1d44080d5bde3a9', targetAccountUuid);
    END LOOP;
    CLOSE cur;
    SELECT CURTIME();
END $$
DELIMITER ;
CALL upgradeProjectOperatorSystemTags();

DROP PROCEDURE IF EXISTS upgradeProjectAdminSystemTags;
DELIMITER $$
CREATE PROCEDURE upgradeProjectAdminSystemTags()
BEGIN
    DECLARE projectAdminTag VARCHAR(59);
    DECLARE targetProjectUuid VARCHAR(32);
    DECLARE targetAccountUuid VARCHAR(32);
    DECLARE iam2VirtualIDUuid VARCHAR(32);
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR SELECT systemTag.tag, systemTag.resourceUuid FROM `zstack`.`SystemTagVO` systemTag where systemTag.tag like 'projectAdminOfProjectUuid::%';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO projectAdminTag, iam2VirtualIDUuid;
        IF done THEN
            LEAVE read_loop;
        END IF;

        SET targetProjectUuid = SUBSTRING_INDEX(projectOperatorTag, '::', 1);
        SELECT `accountUuid` into targetAccountUuid FROM `IAM2ProjectAccountRefVO` WHERE `projectUuid` = targetProjectUuid LIMIT 1;

        INSERT INTO IAM2VirtualIDRoleRefVO (`virtualIDUuid`, `roleUuid`, `targetAccountUuid`) VALUES (iam2VirtualIDUuid, 'f2f474c60e7340c0a1d44080d5bde3a9', targetAccountUuid);
    END LOOP;
    CLOSE cur;
    SELECT CURTIME();
END $$
DELIMITER ;
CALL upgradeProjectAdminSystemTags();


DELIMITER $$
CREATE PROCEDURE insertDefaultIAM2Organization()
BEGIN
    DECLARE virtualIDUuid VARCHAR(32);
    DECLARE organizationUuid VARCHAR(32);
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR SELECT uuid, '6e3d19dab98348d8bd67657378843f82' FROM zstack.IAM2VirtualIDVO where type = 'ZStack' and uuid not in (SELECT virtualIDUuid FROM zstack.IAM2VirtualIDOrganizationRefVO);
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO virtualIDUuid, organizationUuid;
        IF done THEN
            LEAVE read_loop;
        END IF;
        INSERT INTO `zstack`.IAM2VirtualIDOrganizationRefVO (virtualIDUuid, organizationUuid) VALUES (virtualIDUuid, organizationUuid);

    END LOOP;
    CLOSE cur;
    SELECT CURTIME();
END $$
DELIMITER ;

CALL insertDefaultIAM2Organization();
DROP PROCEDURE IF EXISTS insertDefaultIAM2Organization;


CREATE TABLE `IAM2ProjectVirtualIDGroupRefVO` (
    `groupUuid` VARCHAR(32) NOT NULL,
    `projectUuid` VARCHAR(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`groupUuid`,`projectUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DELIMITER $$
CREATE PROCEDURE insertIAM2ProjectVirtualIDGroupRef()
BEGIN
    DECLARE groupUuid VARCHAR(32);
    DECLARE projectUuid VARCHAR(32);
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR SELECT uuid, projectUuid  FROM zstack.IAM2VirtualIDGroupVO;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO groupUuid, projectUuid;
        IF done THEN
            LEAVE read_loop;
        END IF;
        INSERT INTO `zstack`.IAM2ProjectVirtualIDGroupRefVO (groupUuid, projectUuid) VALUES (groupUuid, projectUuid);

    END LOOP;
    CLOSE cur;
    SELECT CURTIME();
END $$
DELIMITER ;

CALL insertIAM2ProjectVirtualIDGroupRef();
DROP PROCEDURE IF EXISTS insertIAM2ProjectVirtualIDGroupRef;


Alter table `zstack`.`IAM2VirtualIDGroupVO` modify projectUuid VARCHAR(32) NULL;

ALTER TABLE IAM2VirtualIDGroupRoleRefVO DROP FOREIGN KEY fkIAM2VirtualIDGroupRoleRefVOIAM2VirtualIDGroupVO;
ALTER TABLE IAM2VirtualIDGroupRoleRefVO DROP FOREIGN KEY fkIAM2VirtualIDGroupRoleRefVORoleVO;
ALTER TABLE IAM2VirtualIDGroupRoleRefVO ADD COLUMN `targetAccountUuid` varchar(32) NOT NULL;
ALTER TABLE IAM2VirtualIDGroupRoleRefVO DROP PRIMARY KEY, ADD PRIMARY KEY(groupUuid, roleUuid, targetAccountUuid);
ALTER TABLE IAM2VirtualIDGroupRoleRefVO ADD CONSTRAINT fkIAM2VirtualIDGroupRoleRefVOIAM2VirtualIDGroupVO FOREIGN KEY (groupUuid) REFERENCES IAM2VirtualIDGroupVO (uuid) ON DELETE CASCADE;
ALTER TABLE IAM2VirtualIDGroupRoleRefVO ADD CONSTRAINT fkIAM2VirtualIDGroupRoleRefVORoleVO FOREIGN KEY (roleUuid) REFERENCES RoleVO (uuid) ON DELETE CASCADE;
CREATE INDEX idxIAM2VirtualIDGroupRoleRefVOTargetAccountUuid ON IAM2VirtualIDGroupRoleRefVO (targetAccountUuid);


update RolePolicyStatementVO set statement = '{"name":"read-apis-for-normal-virtualID","effect":"Allow","actions":["org.zstack.header.cloudformation.APIPreviewResourceStackMsg","org.zstack.header.aliyun.ecs.APIQueryEcsInstanceFromLocalMsg","org.zstack.header.aliyun.oss.APIGetOssBucketFileFromRemoteMsg","org.zstack.vpcfirewall.api.APIQueryFirewallRuleSetMsg","org.zstack.zwatch.api.APIGetAlarmDataMsg","org.zstack.zwatch.thirdparty.api.APIQueryThirdpartyPlatformMsg","org.zstack.core.captcha.APIRefreshCaptchaMsg","org.zstack.iam2.api.APIQueryIAM2VirtualIDMsg","org.zstack.iam2.api.APIGetIAM2VirtualIDAPIPermissionMsg","org.zstack.billing.APICalculateAccountBillingSpendingMsg","org.zstack.header.flowMeter.APIGetVRouterFlowCounterMsg","org.zstack.network.service.lb.APIQueryCertificateMsg","org.zstack.pciDevice.specification.mdev.APIGetMdevDeviceSpecCandidatesMsg","org.zstack.network.service.virtualrouter.APIGetVipUsedPortsMsg","org.zstack.billing.APIQueryAccountBillingMsg","org.zstack.pciDevice.virtual.vfio_mdev.APIGetMdevDeviceCandidatesMsg","org.zstack.header.acl.APIQueryAccessControlListMsg","org.zstack.header.cloudformation.APIQueryStackTemplateMsg","org.zstack.autoscaling.template.APIQueryAutoScalingVmTemplateMsg","org.zstack.header.datacenter.APIGetDataCenterFromRemoteMsg","org.zstack.vpcfirewall.api.APIQueryVpcFirewallMsg","org.zstack.header.volume.APIGetVolumeFormatMsg","org.zstack.header.vm.APIGetVmStartingCandidateClustersHostsMsg","org.zstack.header.buildsystem.APIQueryAppBuildSystemMsg","org.zstack.network.service.virtualrouter.APIGetAttachablePublicL3ForVRouterMsg","org.zstack.faulttolerance.api.APIQueryFaultToleranceVmMsg","org.zstack.header.tag.APIQuerySystemTagMsg","org.zstack.network.service.eip.APIGetEipAttachableVmNicsMsg","org.zstack.network.l2.vxlan.vxlanNetworkPool.APIQueryVniRangeMsg","org.zstack.iam2.api.APIQueryIAM2ProjectAttributeMsg","org.zstack.iam2.api.APILoginIAM2ProjectMsg","org.zstack.iam2.api.APIQueryIAM2ProjectMsg","org.zstack.header.configuration.APIQueryInstanceOfferingMsg","org.zstack.policyRoute.APIQueryPolicyRouteRuleSetL3RefMsg","org.zstack.aliyun.ebs.message.APIQueryAliyunEbsPrimaryStorageMsg","org.zstack.header.vm.APIGetVmMigrationCandidateHostsMsg","org.zstack.vpc.APIGetVpcVRouterDistributedRoutingEnabledMsg","org.zstack.pciDevice.APIGetHostIommuStatusMsg","org.zstack.header.aliyun.network.connection.APIQueryConnectionAccessPointFromLocalMsg","org.zstack.iam2.api.APIQueryIAM2OrganizationAttributeMsg","org.zstack.header.cloudformation.APIGetSupportedCloudFormationResourcesMsg","org.zstack.zwatch.thirdparty.api.APIQuerySNSEndpointThirdpartyAlertHistoryMsg","org.zstack.pciDevice.specification.mdev.APIQueryMdevDeviceSpecMsg","org.zstack.scheduler.APIGetAvailableTriggersMsg","org.zstack.iam2.api.APIGetIAM2ProjectsOfVirtualIDMsg","org.zstack.header.cloudformation.APICheckStackTemplateParametersMsg","org.zstack.header.storage.volume.backup.APIQueryVolumeBackupMsg","org.zstack.header.network.l3.APIGetIpAddressCapacityMsg","org.zstack.header.aliyun.image.APIQueryEcsImageFromLocalMsg","org.zstack.header.storage.snapshot.group.APICheckVolumeSnapshotGroupAvailabilityMsg","org.zstack.zwatch.api.APIGetAllEventMetadataMsg","org.zstack.vpc.APIGetAttachableVpcL3NetworkMsg","org.zstack.header.storage.snapshot.group.APIQueryVolumeSnapshotGroupMsg","org.zstack.network.securitygroup.APIQuerySecurityGroupMsg","org.zstack.header.vm.APIGetCandidateIsoForAttachingVmMsg","org.zstack.header.aliyun.network.group.APIQueryEcsSecurityGroupFromLocalMsg","org.zstack.header.vm.APIGetSpiceCertificatesMsg","org.zstack.header.network.l3.APIQueryIpRangeMsg","org.zstack.resourceconfig.APIQueryResourceConfigMsg","org.zstack.header.network.l3.APIQueryL3NetworkMsg","org.zstack.header.vpc.ha.APIQueryVpcHaGroupMsg","org.zstack.header.buildsystem.APIGetAppBuildSystemCapacityMsg","org.zstack.header.vm.APIGetVmAttachableL3NetworkMsg","org.zstack.iam2.api.APILoginIAM2VirtualIDMsg","org.zstack.header.aliyun.network.vpc.APIQueryEcsVpcFromLocalMsg","org.zstack.header.aliyun.ecs.APIGetEcsInstanceVncUrlMsg","org.zstack.zwatch.api.APIGetAuditDataMsg","org.zstack.header.vm.APIQueryVmPriorityConfigMsg","org.zstack.autoscaling.group.APIQueryAutoScalingGroupMsg","org.zstack.header.apimediator.APIIsReadyToGoMsg","org.zstack.iam2.api.APIQueryIAM2ProjectRoleMsg","org.zstack.header.vm.APIGetVmQgaMsg","org.zstack.header.protocol.APIQueryVRouterOspfAreaMsg","org.zstack.core.config.APIQueryGlobalConfigMsg","org.zstack.aliyunproxy.vpc.APIQueryAliyunProxyVSwitchMsg","org.zstack.network.service.flat.APIGetL3NetworkDhcpIpAddressMsg","org.zstack.network.service.lb.APIGetCandidateL3NetworksForLoadBalancerMsg","org.zstack.ticket.api.APIQueryArchiveTicketMsg","org.zstack.network.service.vip.APIQueryVipMsg","org.zstack.header.hybrid.network.vpn.APIQueryVpcIpSecConfigFromLocalMsg","org.zstack.zwatch.api.APIGetManagementNodeDirCapacityMsg","org.zstack.header.image.APIQueryImageMsg","org.zstack.header.aliyun.storage.snapshot.APIQueryAliyunSnapshotFromLocalMsg","org.zstack.zwatch.api.APIQueryMetricDataHttpReceiverMsg","org.zstack.zwatch.alarm.sns.APIQuerySNSTextTemplateMsg","org.zstack.header.datacenter.APIQueryDataCenterFromLocalMsg","org.zstack.network.service.lb.APIGetCandidateVmNicsForLoadBalancerMsg","org.zstack.header.longjob.APIQueryLongJobMsg","org.zstack.header.network.l2.APIQueryL2VlanNetworkMsg","org.zstack.iam2.api.APIGetIAM2SystemAttributesMsg","org.zstack.network.securitygroup.APIQueryVmNicInSecurityGroupMsg","org.zstack.vrouterRoute.APIQueryVirtualRouterVRouterRouteTableRefMsg","org.zstack.autoscaling.group.rule.APIQueryAutoScalingRuleMsg","org.zstack.header.identity.APIGetResourceAccountMsg","org.zstack.header.aliyun.oss.APIGetOssBackupBucketFromRemoteMsg","org.zstack.sns.APIQuerySNSTopicMsg","org.zstack.autoscaling.group.activity.APIQueryAutoScalingGroupActivityMsg","org.zstack.header.appcenter.APIPreviewResourceFromAppMsg","org.zstack.header.protocol.APIQueryVRouterOspfNetworkMsg","org.zstack.vpc.APIGetVpcVRouterNetworkServiceStateMsg","org.zstack.multicast.router.header.APIGetVpcMulticastRouteMsg","org.zstack.header.volume.APIQueryVolumeMsg","org.zstack.sns.platform.dingtalk.APIQuerySNSDingTalkEndpointMsg","org.zstack.header.aliyun.oss.APIGetOssBucketNameFromRemoteMsg","org.zstack.header.aliyun.network.vrouter.APIQueryAliyunVirtualRouterFromLocalMsg","org.zstack.appliancevm.APIQueryApplianceVmMsg","org.zstack.header.vm.APIGetCandidatePrimaryStoragesForCreatingVmMsg","org.zstack.pciDevice.APIGetPciDeviceCandidatesForNewCreateVmMsg","org.zstack.header.vm.APIQueryVmNicMsg","org.zstack.header.hybrid.network.vpn.APIGetVpcVpnConfigurationFromRemoteMsg","org.zstack.header.network.l3.APIQueryIpAddressMsg","org.zstack.network.l2.vxlan.vxlanNetworkPool.APIQueryL2VxlanNetworkPoolMsg","org.zstack.ticket.api.APIQueryTicketFlowMsg","org.zstack.scheduler.APIGetSchedulerExecutionReportMsg","org.zstack.header.storage.primary.APIQueryPrimaryStorageMsg","org.zstack.header.flowMeter.APIQueryFlowCollectorMsg","org.zstack.iam2.api.APIQueryIAM2VirtualIDGroupAttributeMsg","org.zstack.header.cluster.APIQueryClusterMsg","org.zstack.ticket.api.APIQueryTicketHistoryMsg","org.zstack.header.image.APIGetCandidateBackupStorageForCreatingImageMsg","org.zstack.header.vm.APIGetVmMonitorNumberMsg","org.zstack.header.identity.APIGetSupportedIdentityModelsMsg","org.zstack.policyRoute.APIQueryPolicyRouteRuleSetMsg","org.zstack.header.identity.APIQueryUserGroupMsg","org.zstack.policyRoute.APIQueryPolicyRouteRuleMsg","org.zstack.header.buildapp.APIQueryBuildAppExportHistoryMsg","org.zstack.resourceconfig.APIGetResourceBindableConfigMsg","org.zstack.aliyun.nas.message.APIGetAliyunNasMountTargetRemoteMsg","org.zstack.ha.APIGetVmInstanceHaLevelMsg","org.zstack.header.vm.APIGetVmBootOrderMsg","org.zstack.vpcfirewall.api.APIQueryVpcFirewallVRouterRefMsg","org.zstack.network.service.eip.APIQueryEipMsg","org.zstack.scheduler.APIQuerySchedulerJobMsg","org.zstack.pciDevice.APIGetHostIommuStateMsg","org.zstack.header.identity.APIQueryAccountResourceRefMsg","org.zstack.pciDevice.specification.pci.APIQueryVmInstancePciDeviceSpecRefMsg","org.zstack.zwatch.api.APIGetAllMetricMetadataMsg","org.zstack.vrouterRoute.APIQueryVRouterRouteEntryMsg","org.zstack.zwatch.api.APIGetMetricLabelValueMsg","org.zstack.tag2.APIQueryTagMsg","org.zstack.network.service.flat.APIGetL3NetworkIpStatisticMsg","org.zstack.header.flowMeter.APIQueryFlowMeterMsg","org.zstack.header.volume.APIGetVolumeQosMsg","org.zstack.faulttolerance.api.APIGetFaultToleranceVmsMsg","org.zstack.header.identity.APIQueryQuotaMsg","org.zstack.header.APIIsOpensourceVersionMsg","org.zstack.ticket.api.APIQueryTicketMsg","org.zstack.header.affinitygroup.APIQueryAffinityGroupMsg","org.zstack.header.cloudformation.APIGetResourceFromResourceStackMsg","org.zstack.guesttools.APIGetVmGuestToolsInfoMsg","org.zstack.header.network.l3.APIGetL3NetworkRouterInterfaceIpMsg","org.zstack.network.securitygroup.APIGetCandidateVmNicForSecurityGroupMsg","org.zstack.header.vm.APIGetCandidateZonesClustersHostsForCreatingVmMsg","org.zstack.aliyun.nas.message.APIQueryAliyunNasAccessGroupMsg","org.zstack.license.APIGetLicenseCapabilitiesMsg","org.zstack.vpc.APIGetVpcVRouterDistributedRoutingConnectionsMsg","org.zstack.scheduler.APIQuerySchedulerTriggerMsg","org.zstack.iam2.api.APICheckIAM2VirtualIDConfigFileMsg","org.zstack.storage.ceph.backup.APIQueryCephBackupStorageMsg","org.zstack.header.vm.APIGetVmUsbRedirectMsg","org.zstack.network.service.lb.APIQueryLoadBalancerMsg","org.zstack.pciDevice.APIQueryPciDevicePciDeviceOfferingMsg","org.zstack.header.hybrid.network.eip.APIQueryHybridEipFromLocalMsg","org.zstack.network.l2.vxlan.vxlanNetwork.APIQueryL2VxlanNetworkMsg","org.zstack.ticket.api.APIQueryTicketTypeMsg","org.zstack.header.cloudformation.APIGetResourceStackFromResourceMsg","org.zstack.header.identity.APILogOutMsg","org.zstack.twoFactorAuthentication.APIGetTwoFactorAuthenticationSecretMsg","org.zstack.header.sriov.APIIsVfNicAvailableInL3NetworkMsg","org.zstack.header.network.l3.APIQueryAddressPoolMsg","org.zstack.header.vm.APIGetVmRDPMsg","org.zstack.header.cloudformation.APIQueryResourceStackMsg","org.zstack.iam2.api.APIQueryIAM2ProjectAccountRefMsg","org.zstack.header.vo.APIGetResourceNamesMsg","org.zstack.header.hybrid.account.APIQueryHybridKeySecretMsg","org.zstack.header.flowMeter.APIQueryVRouterFlowMeterNetworkMsg","org.zstack.vpcfirewall.api.APIQueryFirewallRuleMsg","org.zstack.header.volume.APIGetVolumeCapabilitiesMsg","org.zstack.aliyun.nas.message.APIGetAliyunNasAccessGroupRemoteMsg","org.zstack.policyRoute.APIQueryPolicyRouteTableMsg","org.zstack.header.vm.APIGetVmConsolePasswordMsg","org.zstack.vpcfirewall.api.APIQueryFirewallRuleSetL3RefMsg","org.zstack.header.protocol.APIGetVRouterRouterIdMsg","org.zstack.network.service.lb.APIQueryLoadBalancerListenerMsg","org.zstack.header.host.APIQueryHostMsg","org.zstack.accessKey.APIQueryAccessKeyMsg","org.zstack.policyRoute.APIQueryPolicyRouteRuleSetVRouterRefMsg","org.zstack.header.network.l3.APICheckIpAvailabilityMsg","org.zstack.pciDevice.APIQueryPciDeviceMsg","org.zstack.aliyun.nas.message.APIGetAliyunNasFileSystemRemoteMsg","org.zstack.sns.platform.email.APIQuerySNSEmailEndpointMsg","org.zstack.mevoco.APIQueryShareableVolumeVmInstanceRefMsg","org.zstack.header.network.l3.APIGetL3NetworkMtuMsg","org.zstack.header.vm.cdrom.APIQueryVmCdRomMsg","org.zstack.aliyun.ebs.message.APIQueryAliyunEbsBackupStorageMsg","org.zstack.header.vm.APIGetVmHostnameMsg","org.zstack.guesttools.APIGetLatestGuestToolsForVmMsg","org.zstack.iam2.api.APIQueryIAM2OrganizationProjectRefMsg","org.zstack.header.vm.APIGetInterdependentL3NetworksImagesMsg","org.zstack.header.vm.APIGetVmConsoleAddressMsg","org.zstack.header.identity.APIQueryUserMsg","org.zstack.ticket.api.APIQueryTicketFlowCollectionMsg","org.zstack.resourceconfig.APIGetResourceConfigMsg","org.zstack.pciDevice.specification.pci.APIGetPciDeviceSpecCandidatesMsg","org.zstack.network.service.virtualrouter.APIQueryVirtualRouterOfferingMsg","org.zstack.aliyun.pangu.APIQueryAliyunPanguPartitionMsg","org.zstack.scheduler.APIGetNoTriggerSchedulerJobsMsg","org.zstack.header.storage.backup.APIQueryBackupStorageMsg","org.zstack.header.hybrid.network.vpn.APIQueryVpcUserVpnGatewayFromLocalMsg","org.zstack.storage.backup.sftp.APIQuerySftpBackupStorageMsg","org.zstack.ticket.api.APIQueryArchiveTicketHistoryMsg","org.zstack.header.aliyun.network.connection.APIQueryAliyunRouterInterfaceFromLocalMsg","org.zstack.iam2.api.APIQueryIAM2VirtualIDAttributeMsg","org.zstack.header.vm.APIQueryVmInstanceMsg","org.zstack.pciDevice.specification.mdev.APIQueryVmInstanceMdevDeviceSpecRefMsg","org.zstack.iam2.api.APIQueryIAM2ProjectTemplateMsg","org.zstack.header.network.service.APIQueryNetworkServiceProviderMsg","org.zstack.header.appcenter.APIQueryPublishAppMsg","org.zstack.storage.backup.imagestore.APIQueryImageStoreBackupStorageMsg","org.zstack.vrouterRoute.APIGetVRouterRouteTableMsg","org.zstack.header.identity.APIQueryPolicyMsg","org.zstack.header.cloudformation.APIQueryEventFromResourceStackMsg","org.zstack.header.aliyun.network.vpc.APIQueryEcsVSwitchFromLocalMsg","org.zstack.ipsec.APIQueryIPSecConnectionMsg","org.zstack.scheduler.APIQuerySchedulerJobGroupMsg","org.zstack.header.vm.APIGetVmInstanceFirstBootDeviceMsg","org.zstack.twoFactorAuthentication.APIQueryTwoFactorAuthenticationMsg","org.zstack.billing.APICalculateAccountSpendingMsg","org.zstack.sns.APIQuerySNSTopicSubscriberMsg","org.zstack.header.identity.APILogInByAccountMsg","org.zstack.header.volume.APIGetDataVolumeAttachableVmMsg","org.zstack.query.APIBatchQueryMsg","org.zstack.header.hybrid.network.vpn.APIQueryVpcIkeConfigFromLocalMsg","org.zstack.header.aliyun.oss.APIQueryOssBucketFileNameMsg","org.zstack.header.appcenter.APIGetResourceFromPublishAppMsg","org.zstack.header.zone.APIQueryZoneMsg","org.zstack.header.vipQos.APIGetVipQosMsg","org.zstack.storage.primary.local.APILocalStorageGetVolumeMigratableHostsMsg","org.zstack.header.cloudformation.monitor.APIGetResourceStackVmStatusMsg","org.zstack.sns.APIQuerySNSApplicationEndpointMsg","org.zstack.header.aliyun.network.connection.APIQueryVirtualBorderRouterFromLocalMsg","org.zstack.header.flowMeter.APIGetFlowMeterRouterIdMsg","org.zstack.multicast.router.header.APIQueryMulticastRouterMsg","org.zstack.network.service.virtualrouter.APIQueryVirtualRouterVmMsg","org.zstack.header.tag.APIQueryUserTagMsg","org.zstack.policyRoute.APIGetPolicyRouteRuleSetFromVirtualRouterMsg","org.zstack.header.aliyun.network.vrouter.APIQueryAliyunRouteEntryFromLocalMsg","org.zstack.header.storage.snapshot.APIQueryVolumeSnapshotTreeMsg","org.zstack.header.identity.APICheckApiPermissionMsg","org.zstack.aliyunproxy.vpc.APIQueryAliyunProxyVpcMsg","org.zstack.vrouterRoute.APIQueryVRouterRouteTableMsg","org.zstack.header.vm.APIGetVmCapabilitiesMsg","org.zstack.header.protocol.APIGetVRouterOspfNeighborMsg","org.zstack.header.vm.APIGetVmXmlMsg","org.zstack.autoscaling.group.rule.trigger.APIQueryAutoScalingRuleTriggerMsg","org.zstack.pciDevice.APIQueryPciDeviceOfferingMsg","org.zstack.zwatch.api.APIQueryMetricTemplateMsg","org.zstack.header.identity.APIGetAccountQuotaUsageMsg","org.zstack.scheduler.APIQuerySchedulerJobHistoryMsg","org.zstack.header.vm.APIGetVmDeviceAddressMsg","org.zstack.header.aliyun.network.connection.APIGetConnectionAccessPointFromRemoteMsg","org.zstack.header.core.progress.APIGetTaskProgressMsg","org.zstack.header.identity.role.api.APIQueryRoleMsg","org.zstack.network.securitygroup.APIQuerySecurityGroupRuleMsg","org.zstack.zwatch.api.APIGetEventDataMsg","org.zstack.sns.platform.email.APIQuerySNSEmailPlatformMsg","org.zstack.sns.APIQuerySNSSmsEndpointMsg","org.zstack.header.aliyun.network.connection.APIQueryConnectionBetweenL3NetworkAndAliyunVSwitchMsg","org.zstack.iam2.api.APICheckIAM2OrganizationAvailabilityMsg","org.zstack.iam2.api.APIQueryIAM2OrganizationMsg","org.zstack.pciDevice.APIGetPciDeviceCandidatesForAttachingVmMsg","org.zstack.zwatch.alarm.APIQueryAlarmMsg","org.zstack.twoFactorAuthentication.APIGetTwoFactorAuthenticationStateMsg","org.zstack.header.vm.APIGetCandidateVmForAttachingIsoMsg","org.zstack.header.aliyun.network.group.APIQueryEcsSecurityGroupRuleFromLocalMsg","org.zstack.query.APIZQLQueryMsg","org.zstack.header.vm.APIGetImageCandidatesForVmToChangeMsg","org.zstack.header.storage.snapshot.APIQueryVolumeSnapshotMsg","org.zstack.header.vm.APIGetVmAttachableDataVolumeMsg","org.zstack.header.aliyun.storage.disk.APIQueryAliyunDiskFromLocalMsg","org.zstack.header.aliyun.image.APIGetCreateEcsImageProgressMsg","org.zstack.header.network.l3.APIGetL3NetworkTypesMsg","org.zstack.header.configuration.APIQueryDiskOfferingMsg","org.zstack.vmware.APIQueryVCenterBackupStorageMsg","org.zstack.autoscaling.group.instance.APIQueryAutoScalingGroupInstanceMsg","org.zstack.header.buildapp.APICheckBuildAppParametersMsg","org.zstack.sns.platform.http.APIQuerySNSHttpEndpointMsg","org.zstack.zwatch.alarm.APIQueryEventSubscriptionMsg","org.zstack.header.identity.APIQueryAccountMsg","org.zstack.header.hybrid.network.vpn.APIQueryVpcVpnGatewayFromLocalMsg","org.zstack.vpc.APIQueryVpcRouterMsg","org.zstack.header.network.l3.APIGetFreeIpMsg","org.zstack.network.service.portforwarding.APIQueryPortForwardingRuleMsg","org.zstack.header.identity.APILogInByUserMsg","org.zstack.policyRoute.APIQueryPolicyRouteTableVRouterRefMsg","org.zstack.header.aliyun.network.connection.APIGetConnectionBetweenL3NetworkAndAliyunVSwitchMsg","org.zstack.pciDevice.specification.pci.APIQueryPciDeviceSpecMsg","org.zstack.pciDevice.virtual.vfio_mdev.APIQueryMdevDeviceMsg","org.zstack.header.vm.APIGetVmSshKeyMsg","org.zstack.header.network.l2.APIGetL2NetworkTypesMsg","org.zstack.sns.platform.email.APIQuerySNSEmailAddressMsg","org.zstack.iam2.api.APIQueryIAM2VirtualIDGroupMsg","org.zstack.zwatch.alarm.sns.template.aliyunsms.APIQueryAliyunSmsSNSTextTemplateMsg","org.zstack.header.image.APIGetImageQgaMsg","org.zstack.header.vm.APIGetNicQosMsg","org.zstack.header.aliyun.ecs.APIGetEcsInstanceTypeMsg","org.zstack.policyRoute.APIQueryPolicyRouteTableRouteEntryMsg","org.zstack.zwatch.thirdparty.api.APIQueryThirdpartyAlertMsg","org.zstack.header.cloudformation.APIDecodeStackTemplateMsg","org.zstack.header.identity.APIValidateSessionMsg","org.zstack.header.buildapp.APIQueryBuildAppMsg","org.zstack.sns.platform.microsoftteams.APIQuerySNSMicrosoftTeamsEndpointMsg","org.zstack.header.hybrid.network.vpn.APIQueryVpcVpnConnectionFromLocalMsg","org.zstack.storage.backup.imagestore.APIGetImagesFromImageStoreBackupStorageMsg","org.zstack.zwatch.api.APIGetMetricDataMsg","org.zstack.sns.APIQuerySNSApplicationPlatformMsg","org.zstack.network.service.portforwarding.APIGetPortForwardingAttachableVmNicsMsg","org.zstack.header.network.l2.APIQueryL2NetworkMsg","org.zstack.iam2.api.APIUpdateIAM2VirtualIDMsg","org.zstack.core.config.APIQueryGlobalConfigMsg"]}' where statement like '%read-apis-for-normal-virtualID%';
