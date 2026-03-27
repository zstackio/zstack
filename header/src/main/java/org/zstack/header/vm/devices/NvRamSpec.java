package org.zstack.header.vm.devices;

import org.zstack.header.rest.APINoSee;

public class NvRamSpec {
    @APINoSee
    private String backupFileUuid;

    public String getBackupFileUuid() {
        return backupFileUuid;
    }

    public void setBackupFileUuid(String backupFileUuid) {
        this.backupFileUuid = backupFileUuid;
    }
}
