package org.zstack.header.storage.snapshot.group;

import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.RestoreVmMsg;
import org.zstack.header.vm.RestoreVmReply;
import org.zstack.header.vm.SkipVmTracer;

@SkipVmTracer(replyClass = RestoreVmReply.class)
public class RestoreVmFromSnapshotGroupMsg extends NeedReplyMessage implements RestoreVmMsg {
    private String vmInstanceUuid;
    private String groupUuid;
    private Boolean startVm;
    private SessionInventory session;
    private String apiId;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String groupUuid) {
        this.groupUuid = groupUuid;
    }

    @Override
    public Boolean getStartVm() {
        return startVm;
    }

    public void setStartVm(Boolean startVm) {
        this.startVm = startVm;
    }

    public SessionInventory getSession() {
        return session;
    }

    public void setSession(SessionInventory session) {
        this.session = session;
    }

    public String getApiId() {
        return apiId;
    }

    public void setApiId(String apiId) {
        this.apiId = apiId;
    }
}
