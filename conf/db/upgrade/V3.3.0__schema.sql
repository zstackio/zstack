CREATE TABLE IF NOT EXISTS `ElaborationVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `errorInfo` text NOT NULL,
  `md5sum` varchar(32) NOT NULL,
  `distance` double NOT NULL,
  `matched` boolean NOT NULL DEFAULT FALSE,
  `repeats` bigint(20) unsigned NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX idxElaborationVOmd5sum ON ElaborationVO (md5sum);

CREATE TABLE  `zstack`.`VCenterResourcePoolVO` (
    `uuid` varchar(32) NOT NULL COMMENT 'VCenter Resource Pool uuid',
    `vCenterClusterUuid` varchar(32) NOT NULL COMMENT 'VCenter cluster uuid',
    `name` varchar(256) NOT NULL COMMENT 'VCenter Resource Pool name',
    `morVal` varchar(256) NOT NULL COMMENT 'VCenter Resource Pool management object value in vcenter',
    `parentUuid` varchar(32) COMMENT 'Parent Resource Pool uuid or NULL',
    `CPULimit` bigint(64),
    `CPUOverheadLimit` bigint(64),
    `CPUReservation` bigint(64),
    `CPUShares` bigint(64),
    `CPULevel` varchar(64),
    `MemoryLimit` bigint(64),
    `MemoryOverheadLimit` bigint(64),
    `MemoryReservation` bigint(64),
    `MemoryShares` bigint(64),
    `MemoryLevel` varchar(64),
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkVCenterResourcePoolVOVCenterClusterVO` FOREIGN KEY (`vCenterClusterUuid`) REFERENCES `VCenterClusterVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`VCenterResourcePoolUsageVO` (
    `uuid` varchar(32) NOT NULL COMMENT 'VCenter Resource Pool usage uuid',
    `vCenterResourcePoolUuid` varchar(32) NOT NULL COMMENT 'VCenter Resource Pool uuid',
    `resourceUuid` varchar(32) NOT NULL COMMENT 'VCenter Resource resource uuid',
    `resourceType` varchar(256) NOT NULL COMMENT 'VCenter Resource resource type',
    `resourceName` varchar(256) COMMENT 'VCenter Resource resource name',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
     PRIMARY KEY  (`uuid`),
     UNIQUE KEY `VCenterResourcePoolUsageVO` (`vCenterResourcePoolUuid`, `resourceUuid`) USING BTREE,
     CONSTRAINT `fkVCenterResourcePoolUsageVOVCenterResourcePoolVO` FOREIGN KEY (`vCenterResourcePoolUuid`) REFERENCES `VCenterResourcePoolVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# create missing tag2 role for IAM2ProjectVO
DELIMITER $$
CREATE PROCEDURE getRoleUuid(OUT targetRoleUuid VARCHAR(32))
    BEGIN
        SELECT uuid into targetRoleUuid from RoleVO role where role.name = 'predefined: tag2' and role.type = 'Predefined' LIMIT 0,1;
    END $$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE getMaxAccountResourceRefVO(OUT refId bigint(20) unsigned)
    BEGIN
        SELECT max(id) INTO refId from zstack.AccountResourceRefVO;
    END $$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE fixMissingTag2RoleInProjects()
    BEGIN
        DECLARE done INT DEFAULT FALSE;
        DECLARE count_tag_role INT DEFAULT 0;
        DECLARE count_tag_role_for_project INT DEFAULT 0;
        DECLARE targetAccountUuid varchar(32);
        DECLARE targetRoleUuid varchar(32);
        DECLARE new_role_uuid VARCHAR(32);
        DECLARE new_statement_uuid VARCHAR(32);
        DECLARE refId bigint(20) unsigned;
        DECLARE policyStatement text;
        DECLARE cur CURSOR FOR SELECT accountUuid FROM zstack.IAM2ProjectAccountRefVO;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

        OPEN cur;
        CALL getRoleUuid(targetRoleUuid);

        read_loop: LOOP
            FETCH cur INTO targetAccountUuid;
            IF done THEN
                LEAVE read_loop;
            END IF;

            SELECT count(*) into count_tag_role from RoleVO role where role.name = 'predefined: tag2' and role.type = 'Predefined';
            IF (count_tag_role != 0) THEN
               SELECT count(*) into count_tag_role_for_project from RoleVO role, AccountResourceRefVO ref
               where role.name = 'predefined: tag2' and role.type = 'CreatedBySystem'
               and ref.resourceUuid = role.uuid and ref.accountUuid = targetAccountUuid;

               IF (count_tag_role_for_project < 1) THEN
                   SET new_role_uuid = REPLACE(UUID(), '-', '');

                   INSERT INTO ResourceVO (`uuid`, `resourceName`, `resourceType`, `concreteResourceType`)
                   values (new_role_uuid, 'predefined: tag2', 'RoleVO', 'org.zstack.header.identity.role.RoleVO');

                   INSERT INTO RoleVO (`uuid`, `name`, `type`, `state`, `description`, `lastOpDate`, `createDate`)
                   values (new_role_uuid, 'predefined: tag2', 'CreatedBySystem', 'Enabled', NULL, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

                   CALL getMaxAccountResourceRefVO(refId);
                   INSERT INTO AccountResourceRefVO (`id`, `accountUuid`, `ownerAccountUuid`, `resourceUuid`, `resourceType`, `permission`, `isShared`, `lastOpDate`, `createDate`, `concreteResourceType`)
                   values (refId + 1, targetAccountUuid, targetAccountUuid, new_role_uuid, 'RoleVO', 2, 0, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 'org.zstack.header.identity.role.SystemRoleVO');

                   SET new_statement_uuid = REPLACE(UUID(), '-', '');
                   INSERT INTO RolePolicyStatementVO (`uuid`, `statement`, `roleUuid`, `lastOpDate`, `createDate`)
                   values (new_statement_uuid, '{"name":"tag2","effect":"Allow","actions":["org.zstack.tag2.**"]}', new_role_uuid, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
               END IF;
            END IF;
        END LOOP;
        CLOSE cur;
        SELECT CURTIME();
    END $$
DELIMITER ;

call fixMissingTag2RoleInProjects();
DROP PROCEDURE IF EXISTS fixMissingTag2RoleInProjects;
DROP PROCEDURE IF EXISTS getRoleUuid;

DELIMITER $$
CREATE PROCEDURE getProjectAccount(IN targetProjectUuid VARCHAR(32), OUT targetAccountUuid VARCHAR(32))
    BEGIN
        SELECT accountUuid INTO targetAccountUuid from IAM2ProjectAccountRefVO where projectUuid = targetProjectUuid LIMIT 0,1;
    END $$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE checkProjectAdminOfVirtualId(IN virtualUuid VARCHAR(32))
    BEGIN
        DECLARE done INT DEFAULT FALSE;
        DECLARE count_policy INT DEFAULT 0;
        DECLARE count_vid_role_ref INT DEFAULT 0;
        DECLARE targetProjectUuid varchar(32);
        DECLARE targetAccountUuid varchar(32);
        DECLARE refId bigint(20) unsigned;
        DECLARE new_role_uuid varchar(32);
        DECLARE new_statement_uuid varchar(32);
        DECLARE policyStatement text;
        DECLARE role_name VARCHAR(255);
        DECLARE cur CURSOR FOR SELECT value FROM zstack.IAM2VirtualIDAttributeVO where virtualIDUuid = virtualUuid and name = '__ProjectAdmin__';
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

        OPEN cur;
        SET policyStatement = '{"effect":"Allow","actions":["org.zstack.network.service.portforwarding.**","org.zstack.header.vipQos.**","org.zstack.network.service.vip.**","org.zstack.header.apimediator.**","org.zstack.core.config.APIQueryGlobalConfigMsg","org.zstack.storage.primary.local.APILocalStorageGetVolumeMigratableHostsMsg","org.zstack.header.zone.APIQueryZoneMsg","org.zstack.header.storage.primary.APIQueryPrimaryStorageMsg","org.zstack.query.APIBatchQueryMsg","org.zstack.license.APIGetLicenseCapabilitiesMsg","org.zstack.header.storage.backup.APIQueryBackupStorageMsg","org.zstack.storage.primary.local.APILocalStorageMigrateVolumeMsg","org.zstack.query.APIZQLQueryMsg","org.zstack.header.tag.**","org.zstack.header.host.APIQueryHostMsg","org.zstack.header.core.progress.APIGetTaskProgressMsg","org.zstack.header.APIIsOpensourceVersionMsg","org.zstack.header.cloudformation.**","org.zstack.header.longjob.**","org.zstack.header.vo.APIGetResourceNamesMsg","org.zstack.aliyun.pangu.**","org.zstack.header.identity.APIValidateSessionMsg","org.zstack.header.identity.APILogInByUserMsg","org.zstack.header.identity.role.api.APIQueryRoleMsg","org.zstack.header.identity.APIUpdateUserMsg","org.zstack.header.identity.APIQueryUserMsg","org.zstack.header.identity.APILogInByAccountMsg","org.zstack.header.identity.APIDetachPolicyFromUserGroupMsg","org.zstack.header.identity.APIQueryAccountResourceRefMsg","org.zstack.header.identity.APIDeletePolicyMsg","org.zstack.header.identity.role.api.APIRemovePolicyStatementsFromRoleMsg","org.zstack.header.identity.role.api.APIAttachPolicyToRoleMsg","org.zstack.header.identity.APIDetachPolicyFromUserMsg","org.zstack.header.identity.APILogOutMsg","org.zstack.header.identity.role.api.APICreateRoleMsg","org.zstack.header.identity.APIGetResourceAccountMsg","org.zstack.header.identity.role.api.APIUpdateRoleMsg","org.zstack.header.identity.APIDeleteUserGroupMsg","org.zstack.header.identity.APIAttachPoliciesToUserMsg","org.zstack.header.identity.APIQueryUserGroupMsg","org.zstack.header.identity.APIUpdateAccountMsg","org.zstack.header.identity.APIDeleteUserMsg","org.zstack.header.identity.APIDeleteAccountMsg","org.zstack.header.identity.role.api.APIDeleteRoleMsg","org.zstack.header.identity.role.api.APIChangeRoleStateMsg","org.zstack.header.identity.APICreateUserMsg","org.zstack.header.identity.APICreateUserGroupMsg","org.zstack.header.identity.APIQueryPolicyMsg","org.zstack.header.identity.APIAttachPolicyToUserGroupMsg","org.zstack.header.identity.APIQueryQuotaMsg","org.zstack.header.identity.role.api.APIAddPolicyStatementsToRoleMsg","org.zstack.header.identity.APIDetachPoliciesFromUserMsg","org.zstack.header.identity.APIRemoveUserFromGroupMsg","org.zstack.header.identity.APIRenewSessionMsg","org.zstack.header.identity.APICreatePolicyMsg","org.zstack.header.identity.role.api.APIDetachPolicyFromRoleMsg","org.zstack.header.identity.APIGetAccountQuotaUsageMsg","org.zstack.header.identity.APIAddUserToGroupMsg","org.zstack.header.identity.APIUpdateUserGroupMsg","org.zstack.header.identity.APICheckApiPermissionMsg","org.zstack.header.identity.APIQueryAccountMsg","org.zstack.header.identity.APIAttachPolicyToUserMsg","org.zstack.ticket.api.**","org.zstack.network.securitygroup.**","org.zstack.header.vipQos.**","org.zstack.network.service.lb.**","org.zstack.network.service.vip.**","org.zstack.tag2.**","org.zstack.zwatch.**","org.zstack.sns.**","org.zstack.header.volume.APICreateVolumeSnapshotMsg","org.zstack.header.storage.snapshot.**","org.zstack.pciDevice.APIQueryPciDeviceMsg","org.zstack.pciDevice.APIAttachPciDeviceToVmMsg","org.zstack.pciDevice.APIUpdateHostIommuStateMsg","org.zstack.pciDevice.APIDetachPciDeviceFromVmMsg","org.zstack.pciDevice.APIGetPciDeviceCandidatesForNewCreateVmMsg","org.zstack.pciDevice.APIGetPciDeviceCandidatesForAttachingVmMsg","org.zstack.scheduler.**","org.zstack.header.affinitygroup.**","org.zstack.network.service.flat.**","org.zstack.header.network.l2.APIUpdateL2NetworkMsg","org.zstack.network.l2.vxlan.vxlanNetworkPool.APIQueryVniRangeMsg","org.zstack.network.l2.vxlan.vxlanNetwork.APIQueryL2VxlanNetworkMsg","org.zstack.network.l2.vxlan.vxlanNetworkPool.APIQueryL2VxlanNetworkPoolMsg","org.zstack.header.network.l3.**","org.zstack.network.l2.vxlan.vxlanNetwork.APICreateL2VxlanNetworkMsg","org.zstack.header.network.service.APIQueryNetworkServiceProviderMsg","org.zstack.header.network.service.APIAttachNetworkServiceToL3NetworkMsg","org.zstack.header.network.l2.APIDeleteL2NetworkMsg","org.zstack.header.volume.APIChangeVolumeStateMsg","org.zstack.header.volume.APIGetVolumeCapabilitiesMsg","org.zstack.header.volume.APICreateDataVolumeFromVolumeSnapshotMsg","org.zstack.header.volume.APICreateDataVolumeFromVolumeTemplateMsg","org.zstack.header.volume.APIDetachDataVolumeFromVmMsg","org.zstack.header.volume.APISyncVolumeSizeMsg","org.zstack.header.volume.APIResizeDataVolumeMsg","org.zstack.header.volume.APIGetVolumeQosMsg","org.zstack.header.volume.APIQueryVolumeMsg","org.zstack.header.volume.APIAttachDataVolumeToVmMsg","org.zstack.header.volume.APIUpdateVolumeMsg","org.zstack.header.volume.APIResizeRootVolumeMsg","org.zstack.header.volume.APIDeleteDataVolumeMsg","org.zstack.header.volume.APICreateDataVolumeMsg","org.zstack.header.volume.APIDeleteVolumeQosMsg","org.zstack.header.volume.APIExpungeDataVolumeMsg","org.zstack.mevoco.APIQueryShareableVolumeVmInstanceRefMsg","org.zstack.header.volume.APIRecoverDataVolumeMsg","org.zstack.header.volume.APISetVolumeQosMsg","org.zstack.header.volume.APIGetVolumeFormatMsg","org.zstack.header.volume.APIGetDataVolumeAttachableVmMsg","org.zstack.header.storage.volume.backup.**","org.zstack.vpc.**","org.zstack.vrouterRoute.**","org.zstack.header.storage.backup.APIDeleteExportedImageFromBackupStorageMsg","org.zstack.header.image.**","org.zstack.storage.backup.imagestore.APIGetImagesFromImageStoreBackupStorageMsg","org.zstack.header.storage.backup.APIExportImageFromBackupStorageMsg","org.zstack.billing.APICalculateAccountSpendingMsg","org.zstack.header.vipQos.**","org.zstack.ipsec.**","org.zstack.network.service.virtualrouter.**","org.zstack.appliancevm.**","org.zstack.network.service.vip.**","org.zstack.header.vipQos.**","org.zstack.network.service.eip.**","org.zstack.network.service.vip.**","org.zstack.header.console.APIRequestConsoleAccessMsg","org.zstack.sns.**","org.zstack.header.affinitygroup.**","org.zstack.iam2.api.APIRemoveIAM2VirtualIDsFromOrganizationMsg","org.zstack.iam2.api.APIGetIAM2ProjectsOfVirtualIDMsg","org.zstack.iam2.api.APIQueryIAM2OrganizationMsg","org.zstack.iam2.api.APIQueryIAM2ProjectTemplateMsg","org.zstack.iam2.api.APIDeleteIAM2ProjectTemplateMsg","org.zstack.iam2.api.APIQueryIAM2OrganizationAttributeMsg","org.zstack.iam2.api.APIAddRolesToIAM2VirtualIDMsg","org.zstack.iam2.api.APICreateIAM2ProjectTemplateFromProjectMsg","org.zstack.iam2.api.APIAddAttributesToIAM2OrganizationMsg","org.zstack.iam2.api.APICreateIAM2VirtualIDMsg","org.zstack.iam2.api.APIUpdateIAM2ProjectMsg","org.zstack.iam2.api.APIQueryIAM2VirtualIDAttributeMsg","org.zstack.iam2.api.APIChangeIAM2VirtualIDGroupStateMsg","org.zstack.iam2.api.APIChangeIAM2ProjectStateMsg","org.zstack.iam2.api.APIRemoveAttributesFromIAM2OrganizationMsg","org.zstack.iam2.api.APIGetIAM2VirtualIDAPIPermissionMsg","org.zstack.iam2.api.APIRemoveIAM2VirtualIDsFromGroupMsg","org.zstack.iam2.api.APIChangeIAM2VirtualIDStateMsg","org.zstack.iam2.api.APILoginIAM2ProjectMsg","org.zstack.iam2.api.APIRemoveRolesFromIAM2VirtualIDMsg","org.zstack.iam2.api.APIRemoveAttributesFromIAM2VirtualIDGroupMsg","org.zstack.iam2.api.APIRemoveIAM2VirtualIDsFromProjectMsg","org.zstack.iam2.api.APIAddAttributesToIAM2VirtualIDMsg","org.zstack.iam2.api.APILoginIAM2VirtualIDMsg","org.zstack.iam2.api.APIQueryIAM2ProjectAttributeMsg","org.zstack.iam2.api.APICreateIAM2VirtualIDGroupMsg","org.zstack.iam2.api.APIUpdateIAM2VirtualIDGroupMsg","org.zstack.iam2.api.APIDeleteIAM2VirtualIDMsg","org.zstack.iam2.api.APIRemoveRolesFromIAM2VirtualIDGroupMsg","org.zstack.iam2.api.APIAddAttributesToIAM2VirtualIDGroupMsg","org.zstack.iam2.api.APIAddIAM2VirtualIDsToGroupMsg","org.zstack.iam2.api.APICreateIAM2ProjectFromTemplateMsg","org.zstack.iam2.api.APIQueryIAM2ProjectMsg","org.zstack.iam2.api.APIUpdateIAM2VirtualIDAttributeMsg","org.zstack.iam2.api.APIExpungeIAM2ProjectMsg","org.zstack.iam2.api.APIUpdateIAM2VirtualIDGroupAttributeMsg","org.zstack.iam2.api.APIAddIAM2VirtualIDsToOrganizationMsg","org.zstack.iam2.api.APIUpdateIAM2OrganizationAttributeMsg","org.zstack.iam2.api.APIGetIAM2SystemAttributesMsg","org.zstack.iam2.api.APIQueryIAM2VirtualIDGroupMsg","org.zstack.iam2.api.APIAddIAM2VirtualIDsToProjectMsg","org.zstack.iam2.api.APIRecoverIAM2ProjectMsg","org.zstack.iam2.api.APIAddRolesToIAM2VirtualIDGroupMsg","org.zstack.iam2.api.APIQueryIAM2VirtualIDMsg","org.zstack.iam2.api.APIRemoveAttributesFromIAM2VirtualIDMsg","org.zstack.iam2.api.APIUpdateIAM2ProjectAttributeMsg","org.zstack.iam2.api.APIUpdateIAM2VirtualIDMsg","org.zstack.iam2.api.APIQueryIAM2VirtualIDGroupAttributeMsg","org.zstack.iam2.api.APICreateIAM2ProjectTemplateMsg","org.zstack.iam2.api.APIDeleteIAM2VirtualIDGroupMsg","org.zstack.iam2.api.APIRemoveIAM2VirtualIDsFromOrganizationMsg","org.zstack.iam2.api.APIGetIAM2ProjectsOfVirtualIDMsg","org.zstack.iam2.api.APIQueryIAM2OrganizationMsg","org.zstack.iam2.api.APIQueryIAM2ProjectTemplateMsg","org.zstack.iam2.api.APIDeleteIAM2ProjectTemplateMsg","org.zstack.iam2.api.APIQueryIAM2OrganizationAttributeMsg","org.zstack.iam2.api.APIAddRolesToIAM2VirtualIDMsg","org.zstack.iam2.api.APICreateIAM2ProjectTemplateFromProjectMsg","org.zstack.iam2.api.APIAddAttributesToIAM2OrganizationMsg","org.zstack.iam2.api.APICreateIAM2VirtualIDMsg","org.zstack.iam2.api.APIUpdateIAM2ProjectMsg","org.zstack.iam2.api.APIQueryIAM2VirtualIDAttributeMsg","org.zstack.iam2.api.APIChangeIAM2VirtualIDGroupStateMsg","org.zstack.iam2.api.APIChangeIAM2ProjectStateMsg","org.zstack.iam2.api.APIRemoveAttributesFromIAM2OrganizationMsg","org.zstack.iam2.api.APIGetIAM2VirtualIDAPIPermissionMsg","org.zstack.iam2.api.APIRemoveIAM2VirtualIDsFromGroupMsg","org.zstack.iam2.api.APIChangeIAM2VirtualIDStateMsg","org.zstack.iam2.api.APILoginIAM2ProjectMsg","org.zstack.iam2.api.APIRemoveRolesFromIAM2VirtualIDMsg","org.zstack.iam2.api.APIRemoveAttributesFromIAM2VirtualIDGroupMsg","org.zstack.iam2.api.APIRemoveIAM2VirtualIDsFromProjectMsg","org.zstack.iam2.api.APIAddAttributesToIAM2VirtualIDMsg","org.zstack.iam2.api.APILoginIAM2VirtualIDMsg","org.zstack.iam2.api.APIQueryIAM2ProjectAttributeMsg","org.zstack.iam2.api.APICreateIAM2VirtualIDGroupMsg","org.zstack.iam2.api.APIUpdateIAM2VirtualIDGroupMsg","org.zstack.iam2.api.APIDeleteIAM2VirtualIDMsg","org.zstack.iam2.api.APIRemoveRolesFromIAM2VirtualIDGroupMsg","org.zstack.iam2.api.APIAddAttributesToIAM2VirtualIDGroupMsg","org.zstack.iam2.api.APIAddIAM2VirtualIDsToGroupMsg","org.zstack.iam2.api.APICreateIAM2ProjectFromTemplateMsg","org.zstack.iam2.api.APIQueryIAM2ProjectMsg","org.zstack.iam2.api.APIUpdateIAM2VirtualIDAttributeMsg","org.zstack.iam2.api.APIExpungeIAM2ProjectMsg","org.zstack.iam2.api.APIUpdateIAM2VirtualIDGroupAttributeMsg","org.zstack.iam2.api.APIAddIAM2VirtualIDsToOrganizationMsg","org.zstack.iam2.api.APIUpdateIAM2OrganizationAttributeMsg","org.zstack.iam2.api.APIGetIAM2SystemAttributesMsg","org.zstack.iam2.api.APIQueryIAM2VirtualIDGroupMsg","org.zstack.iam2.api.APIAddIAM2VirtualIDsToProjectMsg","org.zstack.iam2.api.APIRecoverIAM2ProjectMsg","org.zstack.iam2.api.APIAddRolesToIAM2VirtualIDGroupMsg","org.zstack.iam2.api.APIQueryIAM2VirtualIDMsg","org.zstack.iam2.api.APIRemoveAttributesFromIAM2VirtualIDMsg","org.zstack.iam2.api.APIUpdateIAM2ProjectAttributeMsg","org.zstack.iam2.api.APIUpdateIAM2VirtualIDMsg","org.zstack.iam2.api.APIQueryIAM2VirtualIDGroupAttributeMsg","org.zstack.iam2.api.APICreateIAM2ProjectTemplateMsg","org.zstack.iam2.api.APIDeleteIAM2VirtualIDGroupMsg","org.zstack.iam2.api.APIUpdateIAM2VirtualIDMsg","org.zstack.header.identity.APIQueryQuotaMsg","org.zstack.iam2.api.APIQueryIAM2VirtualIDMsg","org.zstack.iam2.api.APIQueryIAM2VirtualIDGroupMsg","org.zstack.accessKey.**","org.zstack.header.configuration.APIQueryDiskOfferingMsg","org.zstack.header.configuration.APIQueryInstanceOfferingMsg","org.zstack.ha.**","org.zstack.header.vm.**","org.zstack.header.vm.APICloneVmInstanceMsg","org.zstack.header.vm.APISetVmCleanTrafficMsg","org.zstack.header.vm.APISetVmUsbRedirectMsg","org.zstack.header.vm.APIUpdateVmNicMacMsg","org.zstack.ha.APIDeleteVmInstanceHaLevelMsg","org.zstack.header.vm.APIUpdateVmInstanceMsg","org.zstack.header.vm.APIGetCandidatePrimaryStoragesForCreatingVmMsg","org.zstack.header.vm.APIQueryVmNicMsg","org.zstack.header.vm.APIResumeVmInstanceMsg","org.zstack.header.vm.APISetNicQosMsg","org.zstack.header.vm.APIDeleteVmHostnameMsg","org.zstack.header.vm.APIGetVmConsolePasswordMsg","org.zstack.header.vm.APIDeleteVmSshKeyMsg","org.zstack.header.vm.APIGetVmUsbRedirectMsg","org.zstack.ha.APIGetVmInstanceHaLevelMsg","org.zstack.header.vm.APIGetInterdependentL3NetworksImagesMsg","org.zstack.header.vm.APIMigrateVmMsg","org.zstack.header.vm.APIGetVmBootOrderMsg","org.zstack.header.vm.APIChangeVmPasswordMsg","org.zstack.header.vm.APIDetachIpAddressFromVmNicMsg","org.zstack.header.vm.APISetVmBootOrderMsg","org.zstack.header.vm.APISetVmStaticIpMsg","org.zstack.header.vm.APIGetVmSshKeyMsg","org.zstack.header.vm.APIDetachIsoFromVmInstanceMsg","org.zstack.header.vm.APISetVmRDPMsg","org.zstack.header.vm.APIDestroyVmInstanceMsg","org.zstack.header.vm.APIDeleteVmStaticIpMsg","org.zstack.header.vm.APIAttachIsoToVmInstanceMsg","org.zstack.header.vm.APIDeleteVmNicMsg","org.zstack.header.vm.APIQueryVmInstanceMsg","org.zstack.header.vm.APISetVmHostnameMsg","org.zstack.header.vm.APIGetImageCandidatesForVmToChangeMsg","org.zstack.header.vm.APIGetVmAttachableL3NetworkMsg","org.zstack.header.vm.APIStopVmInstanceMsg","org.zstack.header.vm.APIDeleteVmConsolePasswordMsg","org.zstack.header.vm.APIGetVmCapabilitiesMsg","org.zstack.header.vm.APIDeleteNicQosMsg","org.zstack.header.vm.APIAttachVmNicToVmMsg","org.zstack.header.vm.APIStartVmInstanceMsg","org.zstack.header.vm.APIGetCandidateIsoForAttachingVmMsg","org.zstack.header.vm.APIGetCandidateVmForAttachingIsoMsg","org.zstack.header.vm.APISetVmQgaMsg","org.zstack.header.vm.APISetVmConsolePasswordMsg","org.zstack.header.vm.APIAttachL3NetworkToVmNicMsg","org.zstack.header.vm.APIChangeVmImageMsg","org.zstack.header.vm.APISetVmMonitorNumberMsg","org.zstack.header.vm.APIGetVmAttachableDataVolumeMsg","org.zstack.header.vm.APIGetVmMonitorNumberMsg","org.zstack.header.vm.APIPauseVmInstanceMsg","org.zstack.header.vm.APICreateVmNicMsg","org.zstack.header.vm.APIExpungeVmInstanceMsg","org.zstack.header.vm.APIGetVmStartingCandidateClustersHostsMsg","org.zstack.header.vm.APIGetCandidateZonesClustersHostsForCreatingVmMsg","org.zstack.header.vm.APIRecoverVmInstanceMsg","org.zstack.header.vm.APIGetVmMigrationCandidateHostsMsg","org.zstack.header.vm.APIGetVmQgaMsg","org.zstack.header.vm.APIReimageVmInstanceMsg","org.zstack.header.vm.APIAttachL3NetworkToVmMsg","org.zstack.header.vm.APIGetVmConsoleAddressMsg","org.zstack.header.vm.APIGetVmHostnameMsg","org.zstack.header.vm.APISetVmSshKeyMsg","org.zstack.header.vm.APIGetVmRDPMsg","org.zstack.header.vm.APIRebootVmInstanceMsg","org.zstack.header.vm.APIChangeInstanceOfferingMsg","org.zstack.header.vm.APIDetachL3NetworkFromVmMsg","org.zstack.ha.APISetVmInstanceHaLevelMsg","org.zstack.header.vm.APIGetNicQosMsg"]}';
        read_loop: LOOP
            FETCH cur INTO targetProjectUuid;
            IF done THEN
                LEAVE read_loop;
            END IF;

            SET role_name = CONCAT('project-admin-role-', virtualUuid);
            SELECT count(*) into count_policy from IAM2ProjectAccountRefVO projectAccountRef, RoleVO role,
             AccountResourceRefVO ref where ref.resourceUuid = role.uuid and projectAccountRef.accountUuid =
             ref.accountUuid and projectAccountRef.projectUuid = targetProjectUuid and role.uuid = ref.resourceUuid
             and role.name = role_name;

            IF (count_policy = 1) THEN
                SELECT role.uuid into new_role_uuid from IAM2ProjectAccountRefVO projectAccountRef, RoleVO role,
                 AccountResourceRefVO ref where ref.resourceUuid = role.uuid and projectAccountRef.accountUuid =
                 ref.accountUuid and projectAccountRef.projectUuid = targetProjectUuid and role.uuid = ref.resourceUuid
                 and role.name = role_name;

                SELECT count(*) into count_vid_role_ref from IAM2VirtualIDRoleRefVO where virtualIDUuid = virtualUuid
                 and roleUuid = new_role_uuid;

                IF (count_vid_role_ref < 1) THEN
                    INSERT INTO IAM2VirtualIDRoleRefVO (`virtualIDUuid`, `roleUuid`, `lastOpDate`, `createDate`)
                    values (virtualUuid, new_role_uuid, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
                END IF;
            END IF;

            IF (count_policy < 1) THEN
                SET new_role_uuid = REPLACE(UUID(), '-', '');

                INSERT INTO ResourceVO (`uuid`, `resourceName`, `resourceType`, `concreteResourceType`)
                values (new_role_uuid, role_name, 'RoleVO', 'org.zstack.header.identity.role.RoleVO');

                INSERT INTO RoleVO (`uuid`, `name`, `type`, `state`, `description`, `lastOpDate`, `createDate`)
                values (new_role_uuid, role_name, 'System', 'Enabled', NULL, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

                CALL getProjectAccount(targetProjectUuid, targetAccountUuid);
                CALL getMaxAccountResourceRefVO(refId);
                INSERT INTO AccountResourceRefVO (`id`, `accountUuid`, `ownerAccountUuid`, `resourceUuid`, `resourceType`, `permission`, `isShared`, `lastOpDate`, `createDate`, `concreteResourceType`)
                values (refId + 1, targetAccountUuid, targetAccountUuid, new_role_uuid, 'RoleVO', 2, 0, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 'org.zstack.header.identity.role.SystemRoleVO');

                SET new_statement_uuid = REPLACE(UUID(), '-', '');
                INSERT INTO RolePolicyStatementVO (`uuid`, `statement`, `roleUuid`, `lastOpDate`, `createDate`)
                values (new_statement_uuid, policyStatement, new_role_uuid, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

                INSERT INTO IAM2VirtualIDRoleRefVO (`virtualIDUuid`, `roleUuid`, `lastOpDate`, `createDate`)
                values (virtualUuid, new_role_uuid, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
            END IF;
        END LOOP;
        CLOSE cur;
        SELECT CURTIME();
    END $$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE fixMissingProjectAdminPolicy()
    BEGIN
        DECLARE done INT DEFAULT FALSE;
        DECLARE count_project_admin INT DEFAULT 0;
        DECLARE virtualUuid varchar(32);
        DECLARE refId bigint(20) unsigned;
        DECLARE policyStatement text;
        DECLARE cur CURSOR FOR SELECT uuid FROM zstack.IAM2VirtualIDVO;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO virtualUuid;
            IF done THEN
                LEAVE read_loop;
            END IF;

            SELECT count(*) into count_project_admin from IAM2VirtualIDAttributeVO attr
            where attr.name = '__ProjectAdmin__' and attr.virtualIDUuid = virtualUuid;
            IF (count_project_admin > 0) THEN
                CALL checkProjectAdminOfVirtualId(virtualUuid);
            END IF;
        END LOOP;
        CLOSE cur;
        SELECT CURTIME();
    END $$
DELIMITER ;

# delete dirty project admin attributes in db
delete from IAM2VirtualIDAttributeVO where name = '__ProjectAdmin__' and value not in (select uuid from IAM2ProjectVO);

call fixMissingProjectAdminPolicy();
DROP PROCEDURE IF EXISTS fixMissingProjectAdminPolicy;
DROP PROCEDURE IF EXISTS checkProjectAdminOfVirtualId;
DROP PROCEDURE IF EXISTS getProjectAccount;
DROP PROCEDURE IF EXISTS getMaxAccountResourceRefVO;

CREATE TABLE `VmCdRomVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `name` VARCHAR(256) NOT NULL,
    `vmInstanceUuid` VARCHAR(32) NOT NULL,
    `deviceId` int(10) unsigned NOT NULL COMMENT 'device id',
    `isoUuid` VARCHAR(32) DEFAULT NULL,
    `isoInstallPath` VARCHAR(1024) DEFAULT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `vmInstanceCdRomDeviceId` (`vmInstanceUuid`,`deviceId`),
    KEY `fkVmCdRomVOVmInstanceEO` (`vmInstanceUuid`),
    CONSTRAINT `fkVmCdRomVOVmInstanceEO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkVmCdRomVOImageEO` FOREIGN KEY (`isoUuid`) REFERENCES `ImageEO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE AlarmVO ADD COLUMN `repeatCount` int DEFAULT NULL;
UPDATE `AlarmVO` SET `repeatCount` = -1;

ALTER TABLE `V2VConversionCacheVO` ADD COLUMN `downloadTime` VARCHAR(32);
ALTER TABLE `V2VConversionCacheVO` ADD COLUMN `uploadTime` VARCHAR(32);

DELETE FROM `HostCapacityVO` WHERE uuid NOT IN (SELECT uuid FROM `HostVO`);