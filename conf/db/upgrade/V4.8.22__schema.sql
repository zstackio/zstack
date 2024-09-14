DROP PROCEDURE IF EXISTS check_and_insert_encrypt_metadata;
DELIMITER $$
CREATE PROCEDURE check_and_insert_encrypt_metadata()
BEGIN
    IF (select count(*) from GlobalConfigVO gconfig where gconfig.name = 'enable.password.encrypt' and gconfig.category = 'encrypt' and value != 'None') > 0 THEN
        UPDATE EncryptEntityMetadataVO SET state = 'NewAdded' WHERE entityName = 'IAM2VirtualIDAttributeVO' AND state = 'Encrypted';
        INSERT INTO EncryptEntityMetadataVO (entityName, columnName, state, lastOpDate, createDate) VALUES ('IAM2OrganizationAttributeVO', 'value', 'NeedDecrypt', NOW(), NOW());
        INSERT INTO EncryptEntityMetadataVO (entityName, columnName, state, lastOpDate, createDate) VALUES ('IAM2ProjectAttributeVO', 'value', 'NeedDecrypt', NOW(), NOW());
        INSERT INTO EncryptEntityMetadataVO (entityName, columnName, state, lastOpDate, createDate) VALUES ('IAM2VirtualIDAttributeVO', 'value', 'NeedDecrypt', NOW(), NOW());
        INSERT INTO EncryptEntityMetadataVO (entityName, columnName, state, lastOpDate, createDate) VALUES ('IAM2VirtualIDGroupAttributeVO', 'value', 'NeedDecrypt', NOW(), NOW());
    END IF;
END $$
DELIMITER ;
CALL check_and_insert_encrypt_metadata();

UPDATE `zstack`.`GlobalConfigVO` SET value="64", defaultValue="64" WHERE category="volumeSnapshot" AND name="incrementalSnapshot.maxNum" AND value > 120;