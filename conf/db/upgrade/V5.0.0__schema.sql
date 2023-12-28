UPDATE ImageEO SET md5sum = NULL where md5sum != 'not calculated';
ALTER TABLE `zstack`.`LoadBalancerVO` DROP FOREIGN KEY `fkLoadBalancerVOVipVO`;
ALTER TABLE `zstack`.`LoadBalancerVO` MODIFY COLUMN vipUuid varchar(32) DEFAULT NULL;
ALTER TABLE `zstack`.`LoadBalancerVO` ADD CONSTRAINT `fkLoadBalancerVOVipVO` FOREIGN KEY (`vipUuid`) REFERENCES `zstack`.`VipVO` (`uuid`) ON DELETE SET NULL;

ALTER TABLE `zstack`.`LoadBalancerVO` ADD COLUMN `ipv6VipUuid` varchar(32) DEFAULT null;
ALTER TABLE `zstack`.`LoadBalancerVO` ADD CONSTRAINT `fkLoadBalancerVOIpv6VipVO` FOREIGN KEY (`ipv6VipUuid`) REFERENCES `zstack`.`VipVO` (`uuid`) ON DELETE SET NULL;

ALTER TABLE `zstack`.`LoadBalancerServerGroupVmNicRefVO` ADD COLUMN `ipVersion` int(10) unsigned DEFAULT 4;

UPDATE `zstack`.`VipVO` SET `system` = 0 where `uuid` in (select lb.vipUuid from `zstack`.`LoadBalancerVO` lb, `zstack`.`SlbLoadBalancerVO` slb where lb.uuid = slb.uuid);

update EventSubscriptionVO set name = 'Host Hardware Changed' where uuid = '829d96de006043c3b34202861ca82078';