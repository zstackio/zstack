ALTER TABLE `zstack`.`ModelCenterVO` ADD COLUMN containerStorageNetwork varchar(2048) DEFAULT NULL;
ALTER TABLE `zstack`.`ModelServiceInstanceVO` ADD COLUMN internalUrl varchar(2048) DEFAULT NULL;
ALTER TABLE `zstack`.`ModelServiceInstanceVO` ADD COLUMN k8sResourceYaml mediumtext DEFAULT NULL;

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