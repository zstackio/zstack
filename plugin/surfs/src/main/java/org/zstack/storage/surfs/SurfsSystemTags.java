package org.zstack.storage.surfs;

import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;

/**
 * Created by zhouhaiping on 8/21/2017.
 */
@TagDefinition
public class SurfsSystemTags {
    public static PatternedSystemTag PREDEFINED_BACKUP_STORAGE_POOL = new PatternedSystemTag("surfs::predefinedPool", BackupStorageVO.class);
    public static PatternedSystemTag PREDEFINED_PRIMARY_STORAGE_IMAGE_CACHE_POOL = new PatternedSystemTag("surfs::predefinedImageCachePool", PrimaryStorageVO.class);
    public static PatternedSystemTag PREDEFINED_PRIMARY_STORAGE_ROOT_VOLUME_POOL = new PatternedSystemTag("surfs::predefinedRootVolumePool", PrimaryStorageVO.class);
    public static PatternedSystemTag PREDEFINED_PRIMARY_STORAGE_DATA_VOLUME_POOL = new PatternedSystemTag("surfs::predefinedDataVolumePool", PrimaryStorageVO.class);

    public static final String KVM_SECRET_UUID_TOKEN = "uuid";
    public static PatternedSystemTag KVM_SECRET_UUID = new PatternedSystemTag(String.format("surfs::kvm::secret::{%s}", KVM_SECRET_UUID_TOKEN), PrimaryStorageVO.class);
}
