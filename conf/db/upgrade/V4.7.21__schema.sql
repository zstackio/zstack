CREATE TABLE `zstack`.`RemoteVtepVO` (
  `uuid` varchar(32) NOT NULL UNIQUE,
  `vtepIp` varchar(32) NOT NULL,
  `port` int NOT NULL,
  `clusterUuid` varchar(32) NOT NULL,
  `type` varchar(32) NOT NULL,
  `poolUuid` varchar(32) NOT NULL,
        `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
        `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY  (`uuid`),
        CONSTRAINT fkRemoteVtepVOClusterEO FOREIGN KEY (clusterUuid) REFERENCES ClusterEO (uuid) ON DELETE CASCADE,
        UNIQUE KEY `ukRemoteVtepIpPoolUuid` (`vtepIp`,`poolUuid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

