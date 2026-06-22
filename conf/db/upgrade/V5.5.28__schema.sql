ALTER TABLE `zstack`.`VtepVO` MODIFY COLUMN `vtepIp` varchar(128) NOT NULL;
ALTER TABLE `zstack`.`RemoteVtepVO` MODIFY COLUMN `vtepIp` varchar(128) NOT NULL;

INSERT INTO `zstack`.`SystemTagVO` (`uuid`, `resourceUuid`, `resourceType`, `inherent`, `type`, `tag`, `createDate`, `lastOpDate`)
SELECT REPLACE(UUID(), '-', ''), z.uuid, 'ZoneVO', 0, 'System', 'managementNetwork::ipVersion::ipv4', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()
FROM `zstack`.`ZoneVO` z
WHERE NOT EXISTS (
    SELECT 1
    FROM `zstack`.`SystemTagVO` st
    WHERE st.resourceUuid = z.uuid
      AND st.resourceType = 'ZoneVO'
      AND st.type = 'System'
      AND st.tag LIKE 'managementNetwork::ipVersion::%'
);

ALTER TABLE `zstack`.`ConsoleProxyAgentVO` ADD COLUMN `consoleProxyOverriddenIpv4` varchar(255) DEFAULT NULL;
ALTER TABLE `zstack`.`ConsoleProxyAgentVO` ADD COLUMN `consoleProxyOverriddenIpv6` varchar(255) DEFAULT NULL;
