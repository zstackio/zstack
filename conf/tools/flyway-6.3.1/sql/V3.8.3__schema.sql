ALTER TABLE `zstack`.`VipVO` add column `system` tinyint unsigned NOT NULL DEFAULT 0;

UPDATE ResourceVO SET resourceType = "PrimaryStorageVO", concreteResourceType = "org.zstack.storage.primary.sharedblock.SharedBlockGroupVO"  WHERE resourceType = "SharedBlockGroupVO";
