CALL ADD_COLUMN('VolumeBackupVO', 'hypervisorType', 'varchar(255)', 0, 'kvm');
ALTER TABLE `zstack`.`VolumeBackupHistoryVO` modify column bitmap varchar(32) DEFAULT NULL;