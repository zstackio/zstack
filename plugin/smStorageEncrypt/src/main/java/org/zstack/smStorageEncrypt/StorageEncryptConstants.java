package org.zstack.smStorageEncrypt;

public class StorageEncryptConstants {
    public static final String VOLUME = "volume";
    public static final String VOLUME_UUID = "volumeUuid";
    public static final String ENCRYPT_KEY = "encryptKey";
    public static final String ROOT_VOLUME = "rootVolume";
    public static final String DATA_VOLUMES = "dataVolumes";
    public static final String CACHE_VOLUMES = "cacheVolumes";
    public static final String ENCRYPT_VOLUMES = "encryptVolumes";
    public static final String LIVE = "live";
    public static final String VOLUME_PATH = "volumePath";
    public static final String CREATE_VOLUME_FROM_CACHE_PATH = "/sharedblock/createrootvolume";
    public static final String CREATE_EMPTY_VOLUME_PATH = "/sharedblock/volume/createempty";
    public static final String OFFLINE_MERGE_SNAPSHOT_PATH = "/sharedblock/snapshot/offlinemerge";
    public static final String REVERT_VOLUME_FROM_SNAPSHOT_PATH = "/sharedblock/volume/revertfromsnapshot";
    public static final String SHRINK_SNAPSHOT_PATH = "/sharedblock/snapshot/shrink";
    public static final String CREATE_TEMPLATE_FROM_VOLUME_PATH = "/sharedblock/createtemplatefromvolume";
    public static final String RESIZE_VOLUME_PATH = "/sharedblock/volume/resize";
}

