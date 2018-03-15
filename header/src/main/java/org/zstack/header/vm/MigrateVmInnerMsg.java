package org.zstack.header.vm;

import org.zstack.header.core.ApiTimeout;
import org.zstack.header.image.APIAddImageMsg;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by camile on 3/7/2018.
 * copy by APIMigrateVmMsg for LongJob
 */
@ApiTimeout(apiClasses = {APIMigrateVmMsg.class})
public class MigrateVmInnerMsg extends NeedReplyMessage implements VmInstanceMessage  {
    private String vmInstanceUuid;
    private String hostUuid;
    private Boolean migrateFromDestination;

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public String getVmUuid() {
        return vmInstanceUuid;
    }

    public void setVmUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public Boolean getMigrateFromDestination() {
        return migrateFromDestination;
    }

    public void setMigrateFromDestination(Boolean migrateFromDestination) {
        this.migrateFromDestination = migrateFromDestination;
    }
}
