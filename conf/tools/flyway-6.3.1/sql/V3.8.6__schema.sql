ALTER TABLE `VolumeEO` ADD INDEX idxDeleted (`deleted`);
ALTER TABLE `VmInstanceEO` ADD INDEX idxDeleted (`deleted`);
ALTER TABLE `ImageEO` ADD INDEX idxDeleted (`deleted`);
ALTER TABLE `VolumeSnapshotEO` ADD INDEX idxDeleted (`deleted`);
ALTER TABLE `InstanceOfferingEO` ADD INDEX idxDeleted (`deleted`);