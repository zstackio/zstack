package org.zstack.header.storage.backup;

import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.zone.ZoneVO;

import java.util.List;

/**
 */
public class APIGetBackupStorageCapacityMsg extends APISyncCallMessage {
    @APIParam(required = false, resourceType = ZoneVO.class)
    private List<String> zoneUuids;
    @APIParam(required = false, resourceType = BackupStorageVO.class)
    private List<String> backupStorageUuids;
    private boolean all;

    public boolean isAll() {
        return all;
    }

    public void setAll(boolean all) {
        this.all = all;
    }

    public List<String> getZoneUuids() {
        return zoneUuids;
    }

    public void setZoneUuids(List<String> zoneUuids) {
        this.zoneUuids = zoneUuids;
    }

    public List<String> getBackupStorageUuids() {
        return backupStorageUuids;
    }

    public void setBackupStorageUuids(List<String> backupStorageUuids) {
        this.backupStorageUuids = backupStorageUuids;
    }
}
