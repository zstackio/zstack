package org.zstack.storage.backup;

import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;

/**
 * Created by mingjian.deng on 2017/9/22.
 */
@TagDefinition
public class BackupStorageSystemTags {
    public static String BACKUP_STORE_DEPLOY_REMOTE_TOKEN = "remote";
    public static PatternedSystemTag BACKUP_STORE_DEPLOY_REMOTE = new PatternedSystemTag(String.format("%s", BACKUP_STORE_DEPLOY_REMOTE_TOKEN), BackupStorageVO.class);

    public static final String BACKUP_STORAGE_DATA_NETWORK_TOKEN = "backupStorageDataNetwork";
    public static PatternedSystemTag BACKUP_STORAGE_DATA_NETWORK = new PatternedSystemTag(String.format("backupStorage::data::network::cidr::{%s}", BACKUP_STORAGE_DATA_NETWORK_TOKEN), BackupStorageVO.class);

    public static String EXTRA_IPS_TOKEN = "extraips";
    public static PatternedSystemTag EXTRA_IPS = new PatternedSystemTag(String.format("extraips::{%s}", EXTRA_IPS_TOKEN), BackupStorageVO.class);
}
