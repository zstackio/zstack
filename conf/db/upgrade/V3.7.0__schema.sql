CREATE TABLE `zstack`.`VmPriorityConfigVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `level` VARCHAR(255) NOT NULL UNIQUE,
    `cpuShares` int NOT NULL,
    `oomScoreAdj` int NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;