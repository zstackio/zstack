ALTER TABLE `RestAPIVO`
    ADD COLUMN `ownerAccountUuid` varchar(32) DEFAULT NULL,
    ADD COLUMN `ownerUserUuid` varchar(32) DEFAULT NULL;
