ALTER TABLE `zstack`.`AccessControlListEntryVO` ADD COLUMN `type` varchar (32) NOT NULL DEFAULT 'IpEntry';
ALTER TABLE `zstack`.`AccessControlListEntryVO` MODIFY COLUMN `ipEntries` varchar (2048) DEFAULT NULL;
ALTER TABLE `zstack`.`AccessControlListEntryVO` ADD COLUMN `name` varchar (32) DEFAULT NULL;
ALTER TABLE `zstack`.`AccessControlListEntryVO` ADD COLUMN `matchMethod` varchar (32) DEFAULT NULL;
ALTER TABLE `zstack`.`AccessControlListEntryVO` ADD COLUMN `criterion` varchar (32) DEFAULT NULL;
ALTER TABLE `zstack`.`AccessControlListEntryVO` ADD COLUMN `domain` varchar (255) DEFAULT NULL;
ALTER TABLE `zstack`.`AccessControlListEntryVO` ADD COLUMN `url` varchar (255) DEFAULT NULL;
ALTER TABLE `zstack`.`AccessControlListEntryVO` ADD COLUMN `redirectRule` varchar (1024) DEFAULT NULL;

ALTER TABLE `zstack`.`LoadBalancerListenerACLRefVO` ADD COLUMN `serverGroupUuid` varchar (32) DEFAULT NULL;
ALTER TABLE `zstack`.`LoadBalancerListenerACLRefVO` ADD CONSTRAINT fkLoadBalancerListenerACLRefVOLoadBalancerServerGroupVO FOREIGN KEY (serverGroupUuid) REFERENCES `zstack`.`LoadBalancerServerGroupVO` (uuid) ON DELETE CASCADE;

ALTER TABLE `zstack`.`LoadBalancerListenerACLRefVO` DROP FOREIGN KEY fkLoadbalancerListenerACLRefVOAccessControlListVO;
ALTER TABLE `zstack`.`LoadBalancerListenerACLRefVO` ADD CONSTRAINT fkLoadbalancerListenerACLRefVOAccessControlListVO FOREIGN KEY (aclUuid) REFERENCES `zstack`.`AccessControlListVO` (uuid) ON DELETE CASCADE;
