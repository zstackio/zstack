package org.zstack.storage.fusionstor;

import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;

/**
 * Created by frank on 8/11/2015.
 */
@TagDefinition
public class FusionstorSystemTags {
    public static PatternedSystemTag PREDEFINED_BACKUP_STORAGE_POOL = new PatternedSystemTag("fusionstor::predefinedPool", BackupStorageVO.class);
    public static PatternedSystemTag PREDEFINED_PRIMARY_STORAGE_IMAGE_CACHE_POOL = new PatternedSystemTag("fusionstor::predefinedImageCachePool", PrimaryStorageVO.class);
    public static PatternedSystemTag PREDEFINED_PRIMARY_STORAGE_ROOT_VOLUME_POOL = new PatternedSystemTag("fusionstor::predefinedRootVolumePool", PrimaryStorageVO.class);
    public static PatternedSystemTag PREDEFINED_PRIMARY_STORAGE_DATA_VOLUME_POOL = new PatternedSystemTag("fusionstor::predefinedDataVolumePool", PrimaryStorageVO.class);

    public static final String KVM_SECRET_UUID_TOKEN = "uuid";
    public static PatternedSystemTag KVM_SECRET_UUID = new PatternedSystemTag(String.format("fusionstor::kvm::secret::{%s}", KVM_SECRET_UUID_TOKEN), PrimaryStorageVO.class);
}
