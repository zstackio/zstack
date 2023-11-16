package org.zstack.iscsi;

import org.zstack.compute.host.HostSystemTags;
import org.zstack.header.host.HostVO;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.storage.backup.BackupStorageSystemTags;

public class IscsiUtils {
    public static String getHostInitiatorName(String hostUuid) {
        return HostSystemTags.ISCSI_INITIATOR_NAME.getTokenByResourceUuid(hostUuid, HostVO.class, HostSystemTags.ISCSI_INITIATOR_NAME_TOKEN);
    }

    public static String getBSInitiatorName(String bsUuid) {
        return BackupStorageSystemTags.ISCSI_INITIATOR_NAME.getTokenByResourceUuid(bsUuid, BackupStorageVO.class, BackupStorageSystemTags.ISCSI_INITIATOR_NAME_TOKEN);
    }
}
