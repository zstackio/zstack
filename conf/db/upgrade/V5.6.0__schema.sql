ALTER TABLE `ScsiLunVmInstanceRefVO` ADD COLUMN `diskMode` VARCHAR(32) NOT NULL DEFAULT 'scsi-block';

CALL ADD_COLUMN('LogServerVO', 'connectionStatus', 'VARCHAR(32)', 1, NULL);
CALL ADD_COLUMN('LogServerVO', 'connectionTestUuid', 'VARCHAR(32)', 1, NULL);
CALL ADD_COLUMN('LogServerVO', 'lastConnectionTestDate', 'TIMESTAMP NULL', 1, NULL);
