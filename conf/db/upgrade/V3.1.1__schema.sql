ALTER TABLE CephPrimaryStoragePoolVO ADD totalCapacity bigint(20) unsigned NOT NULL DEFAULT 0;

-- ----------------------------
--  for external baremetal pxe server
-- ----------------------------
DELETE FROM `zstack`.`BaremetalPxeServerVO`;
ALTER TABLE `zstack`.`BaremetalPxeServerVO` ADD COLUMN `zoneUuid` varchar(32) NOT NULL;
ALTER TABLE `zstack`.`BaremetalPxeServerVO` ADD COLUMN `hostname` varchar(255) NOT NULL UNIQUE;
ALTER TABLE `zstack`.`BaremetalPxeServerVO` ADD COLUMN `sshUsername` varchar(64) NOT NULL;
ALTER TABLE `zstack`.`BaremetalPxeServerVO` ADD COLUMN `sshPassword` varchar(255) NOT NULL;
ALTER TABLE `zstack`.`BaremetalPxeServerVO` ADD COLUMN `sshPort` int unsigned NOT NULL;
ALTER TABLE `zstack`.`BaremetalPxeServerVO` ADD COLUMN `storagePath` varchar(2048) NOT NULL;
ALTER TABLE `zstack`.`BaremetalPxeServerVO` ADD COLUMN `totalCapacity` bigint unsigned DEFAULT 0;
ALTER TABLE `zstack`.`BaremetalPxeServerVO` ADD COLUMN `availableCapacity` bigint unsigned DEFAULT 0;
ALTER TABLE `zstack`.`BaremetalPxeServerVO` ADD COLUMN `dhcpInterfaceAddress` varchar(32) DEFAULT NULL;
ALTER TABLE `zstack`.`BaremetalPxeServerVO` ADD COLUMN `state` varchar(32) NOT NULL;
ALTER TABLE `zstack`.`BaremetalPxeServerVO` DROP INDEX `dhcpInterface`;

DELETE FROM `zstack`.`BaremetalImageCacheVO`;
ALTER TABLE `zstack`.`BaremetalImageCacheVO` ADD COLUMN `pxeServerUuid` varchar(32) NOT NULL;
ALTER TABLE `zstack`.`BaremetalImageCacheVO` ADD CONSTRAINT fkBaremetalImageCacheVOBaremetalPxeServerVO FOREIGN KEY (pxeServerUuid) REFERENCES BaremetalPxeServerVO (uuid) ON DELETE CASCADE;

ALTER TABLE `zstack`.`BaremetalChassisVO` ADD COLUMN `pxeServerUuid` varchar(32) DEFAULT NULL;
ALTER TABLE `zstack`.`BaremetalChassisVO` ADD CONSTRAINT fkBaremetalChassisVOBaremetalPxeServerVO FOREIGN KEY (pxeServerUuid) REFERENCES BaremetalPxeServerVO (uuid) ON DELETE SET NULL;
ALTER TABLE `zstack`.`BaremetalInstanceVO` ADD COLUMN `pxeServerUuid` varchar(32) DEFAULT NULL;
ALTER TABLE `zstack`.`BaremetalInstanceVO` ADD CONSTRAINT fkBaremetalInstanceVOBaremetalPxeServerVO FOREIGN KEY (pxeServerUuid) REFERENCES BaremetalPxeServerVO (uuid) ON DELETE SET NULL;

CREATE TABLE  `zstack`.`BaremetalPxeServerClusterRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `clusterUuid` varchar(32) NOT NULL,
    `pxeServerUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`id`),
    CONSTRAINT fkBaremetalPxeServerClusterRefVOClusterEO FOREIGN KEY (clusterUuid) REFERENCES ClusterEO (uuid) ON DELETE CASCADE,
    CONSTRAINT fkBaremetalPxeServerClusterRefVOBaremetalPxeServerVO FOREIGN KEY (pxeServerUuid) REFERENCES BaremetalPxeServerVO (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


-- ----------------------------
--  repair zstack resourcevo caused by db/upgrade/V2.4.0__schema.sql #generateVpcRouterVmVO
-- ----------------------------

DROP TABLE IF EXISTS temp_array_table;
CREATE TABLE temp_array_table (idx INT, value VARCHAR(128));
INSERT INTO temp_array_table (idx, value) VALUES (1, 'AccountVO') ,(2, 'AffinityGroupVO') ,(3, 'AlarmVO') ,(4, 'AlertVO') ,(5, 'AliyunDiskVO') ,(6, 'AliyunNasAccessGroupVO') ,(7, 'AliyunNasAccessRuleVO') ,(8, 'AliyunRouterInterfaceVO') ,(9, 'AliyunSnapshotVO') ,(10, 'BackupStorageVO') ,(11, 'BaremetalChassisVO') ,(12, 'BaremetalPxeServerVO') ,(13, 'CephMonVO') ,(14, 'CephPrimaryStoragePoolVO') ,(15, 'CertificateVO') ,(16, 'ClusterVO') ,(17, 'ConnectionAccessPointVO') ,(18, 'ConsoleProxyVO') ,(19, 'DahoCloudConnectionVO') ,(20, 'DahoConnectionVO') ,(21, 'DahoVllsVO') ,(22, 'DataCenterVO') ,(23, 'DiskOfferingVO') ,(24, 'EcsImageVO') ,(25, 'EcsInstanceVO') ,(26, 'EcsSecurityGroupRuleVO') ,(27, 'EcsSecurityGroupVO') ,(28, 'EcsVSwitchVO') ,(29, 'EcsVpcVO') ,(30, 'EipVO') ,(31, 'EventSubscriptionVO') ,(32, 'GarbageCollectorVO') ,(33, 'HostVO') ,(34, 'HybridAccountVO') ,(35, 'HybridEipAddressVO') ,(36, 'IAM2OrganizationVO') ,(37, 'IAM2ProjectTemplateVO') ,(38, 'IAM2ProjectVO') ,(39, 'IAM2VirtualIDGroupVO') ,(40, 'IAM2VirtualIDVO') ,(41, 'IPsecConnectionVO') ,(42, 'IdentityZoneVO') ,(43, 'ImageVO') ,(44, 'InstanceOfferingVO') ,(45, 'IpRangeVO') ,(46, 'L2NetworkVO') ,(47, 'L3NetworkVO') ,(48, 'LdapServerVO') ,(49, 'LoadBalancerListenerVO') ,(50, 'LoadBalancerVO') ,(51, 'LongJobVO') ,(52, 'MediaVO') ,(53, 'MonitorTriggerActionVO') ,(54, 'MonitorTriggerVO') ,(55, 'NasFileSystemVO') ,(56, 'NasMountTargetVO') ,(57, 'OssBucketVO') ,(58, 'PciDeviceOfferingVO') ,(59, 'PciDeviceVO') ,(60, 'PolicyVO') ,(61, 'PortForwardingRuleVO') ,(62, 'PrimaryStorageVO') ,(63, 'QuotaVO') ,(64, 'RoleVO') ,(65, 'SNSApplicationEndpointVO') ,(66, 'SNSApplicationPlatformVO') ,(67, 'SNSDingTalkAtPersonVO') ,(68, 'SNSTextTemplateVO') ,(69, 'SNSTopicVO') ,(70, 'SchedulerJobVO') ,(71, 'SchedulerTriggerVO') ,(72, 'SchedulerVO') ,(73, 'SecurityGroupRuleVO') ,(74, 'SecurityGroupVO') ,(75, 'SharedBlockVO') ,(76, 'TicketStatusHistoryVO') ,(77, 'TicketVO') ,(78, 'UsbDeviceVO') ,(79, 'UserGroupVO') ,(80, 'UserVO') ,(81, 'VCenterDatacenterVO') ,(82, 'VCenterVO') ,(83, 'VRouterRouteEntryVO') ,(84, 'VRouterRouteTableVO') ,(85, 'VipVO') ,(86, 'VirtualBorderRouterVO') ,(87, 'VmInstanceVO') ,(88, 'VmNicVO') ,(89, 'VniRangeVO') ,(90, 'VolumeVO') ,(91, 'VolumeSnapshotVO') ,(92, 'VolumeSnapshotTreeVO') ,(93, 'VpcUserVpnGatewayVO') ,(94, 'VpcVirtualRouteEntryVO') ,(95, 'VpcVirtualRouterVO') ,(96, 'VpcVpnConnectionVO') ,(97, 'VpcVpnGatewayVO') ,(98, 'VpcVpnIkeConfigVO') ,(99, 'VpcVpnIpSecConfigVO') ,(100, 'VtepVO') ,(101, 'ZoneVO');
DROP PROCEDURE IF EXISTS doFixResourceVOResourceType;
DELIMITER $$
CREATE PROCEDURE doFixResourceVOResourceType()
    BEGIN
        DECLARE tableName varchar(128);
        DECLARE done INT DEFAULT FALSE;
        DECLARE count_table INT DEFAULT 0;
        DECLARE cur CURSOR FOR SELECT value FROM temp_array_table;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO tableName;
            IF done THEN
                LEAVE read_loop;
            END IF;

            SELECT COUNT(*) INTO count_table FROM information_schema.tables WHERE table_schema='zstack' AND table_name=tableName;

            IF (count_table < 1) THEN
                iterate read_loop;
            end if;

            set @s = CONCAT('update ignore zstack.ResourceVO set ResourceVO.resourceType=\'', tableName, '\' where ResourceVO.resourceType=\'VpcRouterVmVO\' and ResourceVO.uuid in (select uuid from ', tableName, ')');
            prepare stmt from @s;
            execute stmt;
            deallocate prepare stmt;

        END LOOP;
        CLOSE cur;
        SELECT CURTIME();
    END $$
DELIMITER ;

DROP PROCEDURE IF EXISTS fixResourceVOResourceType;
DELIMITER $$
CREATE PROCEDURE fixResourceVOResourceType()
    BEGIN
        DECLARE count_admin INT DEFAULT 0;

        SELECT count(*) into count_admin from ResourceVO r, AccountVO a where a.uuid=r.uuid and a.name='admin' and r.resourceType='VpcRouterVmVO';
        IF (count_admin > 0) THEN
            call doFixResourceVOResourceType();
        end if;

        SELECT CURTIME();
    END $$
DELIMITER ;
call fixResourceVOResourceType();