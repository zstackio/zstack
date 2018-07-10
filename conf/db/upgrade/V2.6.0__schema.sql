SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE AccountResourceRefVO ADD COLUMN `concreteResourceType` varchar(512) NOT NULL;
UPDATE AccountResourceRefVO set concreteResourceType = 'org.zstack.network.l2.vxlan.vxlanNetwork', resourceType = 'L2NetworkVO' WHERE resourceType = 'VxlanNetworkVO';
ALTER TABLE ResourceVO ADD COLUMN `concreteResourceType` varchar(512) NOT NULL;

SET FOREIGN_KEY_CHECKS = 1;