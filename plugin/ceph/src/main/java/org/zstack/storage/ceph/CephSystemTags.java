package org.zstack.storage.ceph;

import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.header.volume.VolumeVO;
import org.zstack.tag.PatternedSystemTag;

/**
 * Created by frank on 8/11/2015.
 */
@TagDefinition
public class CephSystemTags {
    public static PatternedSystemTag PREDEFINED_BACKUP_STORAGE_POOL = new PatternedSystemTag("ceph::predefinedPool", BackupStorageVO.class);
    public static PatternedSystemTag PREDEFINED_PRIMARY_STORAGE_IMAGE_CACHE_POOL = new PatternedSystemTag("ceph::predefinedImageCachePool", PrimaryStorageVO.class);
    public static PatternedSystemTag PREDEFINED_PRIMARY_STORAGE_ROOT_VOLUME_POOL = new PatternedSystemTag("ceph::predefinedRootVolumePool", PrimaryStorageVO.class);
    public static PatternedSystemTag PREDEFINED_PRIMARY_STORAGE_DATA_VOLUME_POOL = new PatternedSystemTag("ceph::predefinedDataVolumePool", PrimaryStorageVO.class);

    public static final String USE_CEPH_PRIMARY_STORAGE_POOL_TOKEN = "poolName";
    public static PatternedSystemTag USE_CEPH_PRIMARY_STORAGE_POOL = new PatternedSystemTag(String.format("ceph::pool::{%s}", USE_CEPH_PRIMARY_STORAGE_POOL_TOKEN), VolumeVO.class);

    public static final String KVM_SECRET_UUID_TOKEN = "uuid";
    public static PatternedSystemTag KVM_SECRET_UUID = new PatternedSystemTag(String.format("ceph::kvm::secret::{%s}", KVM_SECRET_UUID_TOKEN), PrimaryStorageVO.class);

    public static final String NO_CEPHX_TOKEN = "ceph::nocephx";
    public static PatternedSystemTag NO_CEPHX = new PatternedSystemTag(NO_CEPHX_TOKEN, PrimaryStorageVO.class);
}
