package org.zstack.header.vm;

import org.zstack.header.message.NeedQuotaCheckMessage;
import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 9:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class StartVmInstanceMsg extends NeedReplyMessage implements VmInstanceMessage, NeedQuotaCheckMessage {
    private String vmInstanceUuid;
    private String accountUuid;
    private List<String> softAvoidHostUuids;

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public List<String> getSoftAvoidHostUuids() {
        return softAvoidHostUuids;
    }

    public void setSoftAvoidHostUuids(List<String> softAvoidHostUuids) {
        this.softAvoidHostUuids = softAvoidHostUuids;
    }
}
