package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.primary.PrimaryStorageMessage;
import org.zstack.header.vm.VmAccountPerference;

/**
 * Created by mingjian.deng on 16/10/26.
 */
public class SetRootPasswordMsg extends NeedReplyMessage implements HostMessage, PrimaryStorageMessage{
    private String hostUuid;
    private String primaryStorageUuid;
    private String qcowFile;
    private VmAccountPerference accountPerference;

    public VmAccountPerference getVmAccountPerference() {
        return accountPerference;
    }

    public void setVmAccountPerference(VmAccountPerference accountPerference) {
        this.accountPerference = accountPerference;
    }

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getQcowFile() {
        return qcowFile;
    }

    public void setQcowFile(String qcowFile) {
        this.qcowFile = qcowFile;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }
}
