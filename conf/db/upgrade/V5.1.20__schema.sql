ALTER TABLE `zstack`.`ModelCenterVO` ADD COLUMN containerStorageNetwork varchar(2048) DEFAULT NULL;
ALTER TABLE `zstack`.`ModelServiceInstanceVO` ADD COLUMN internalUrl varchar(2048) DEFAULT NULL;
ALTER TABLE `zstack`.`ModelServiceInstanceVO` ADD COLUMN k8sResourceYaml mediumtext DEFAULT NULL;
ALTER TABLE `zstack`.`ModelServiceInstanceVO` ADD COLUMN urlMaps mediumtext DEFAULT NULL;

DROP PROCEDURE IF EXISTS UpdateK8sResourceYaml;
DELIMITER //
CREATE PROCEDURE UpdateK8sResourceYaml()
BEGIN
    -- 开始事务
    START TRANSACTION;

    UPDATE ModelServiceInstanceVO
    SET k8sResourceYaml = yaml
    WHERE vmInstanceUuid IS NULL AND k8sResourceYaml IS NULL AND yaml IS NOT NULL;

    -- 提交事务
    COMMIT;
END //
DELIMITER ;
CALL UpdateK8sResourceYaml();

CREATE TABLE IF NOT EXISTS `zstack`.`ApplicationDevelopmentServiceVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NULL,
    `modelServiceGroupUuid` varchar(32) NULL,
    `modelServiceUuid` varchar(32) NULL,
    `deploymentStatus` varchar(255) NOT NULL,
    CONSTRAINT fkApplicationDevelopmentServiceVOModelServiceGroupVO FOREIGN KEY (modelServiceGroupUuid) REFERENCES ModelServiceInstanceGroupVO (uuid) ON DELETE SET NULL,
  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
