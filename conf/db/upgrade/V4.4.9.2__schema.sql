CREATE TABLE IF NOT EXISTS `zstack`.`WestoneSecretResourcePoolVO` (
    `uuid` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `tenantId` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `appId` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `secret` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `initParamUrl` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `initParamWorkdId` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `initParamWorkdir` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    PRIMARY KEY (`uuid`) USING BTREE,
    UNIQUE INDEX `uuid`(`uuid`) USING BTREE,
    CONSTRAINT `fkWestoneSecretResourcePoolVOSecretResourcePoolVO` FOREIGN KEY (`uuid`) REFERENCES `SecretResourcePoolVO` (`uuid`) ON DELETE CASCADE ON UPDATE RESTRICT
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`WestoneSecurityMachineVO` (
    `uuid` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `port` int UNSIGNED NOT NULL,
    `password` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    PRIMARY KEY (`uuid`) USING BTREE,
    UNIQUE INDEX `uuid`(`uuid`) USING BTREE,
    CONSTRAINT `fkWestoneSecurityMachineVOSecurityMachineVO` FOREIGN KEY (`uuid`) REFERENCES `SecurityMachineVO` (`uuid`) ON DELETE CASCADE ON UPDATE RESTRICT
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8;
CREATE TABLE IF NOT EXISTS `zstack`.`ZhongfuSecretResourcePoolVO` (
    `uuid` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    PRIMARY KEY (`uuid`) USING BTREE,
    UNIQUE INDEX `uuid`(`uuid`) USING BTREE,
    CONSTRAINT `fkZhongfuSecretResourcePoolVOSecretResourcePoolVO` FOREIGN KEY (`uuid`) REFERENCES `SecretResourcePoolVO` (`uuid`) ON DELETE CASCADE ON UPDATE RESTRICT
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8;