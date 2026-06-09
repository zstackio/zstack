package org.zstack.storage.ceph.primary;

public enum CephImageCachePoolStrategy {
    DefaultImageCachePool,
    PreferVolumePool,
    PreferExistingCache
}
