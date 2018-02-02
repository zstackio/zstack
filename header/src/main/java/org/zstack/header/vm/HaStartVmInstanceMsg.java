package org.zstack.header.vm;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

/**
 * Created by xing5 on 2016/3/29.
 */
public class HaStartVmInstanceMsg extends NeedReplyMessage implements VmInstanceMessage {
    private String vmInstanceUuid;
    private String judgerClassName;
    private List<String> softAvoidHostUuids;

    public String getJudgerClassName() {
        return judgerClassName;
    }

    public void setJudgerClassName(String judgerClassName) {
        this.judgerClassName = judgerClassName;
    }

    public List<String> getSoftAvoidHostUuids() {
        return softAvoidHostUuids;
    }

    public void setSoftAvoidHostUuids(List<String> softAvoidHostUuids) {
        this.softAvoidHostUuids = softAvoidHostUuids;
    }

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
