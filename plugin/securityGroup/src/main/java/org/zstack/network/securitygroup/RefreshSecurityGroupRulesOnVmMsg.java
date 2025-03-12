package org.zstack.network.securitygroup;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.VmInstanceConstant;

import java.util.List;

public class RefreshSecurityGroupRulesOnVmMsg extends NeedReplyMessage {
    private String vmInstanceUuid;
    private String hostUuid;
    private boolean deleteAllRules;
    private List<String> vNicUuids;
    private List<String> sgUuids;
    private VmInstanceConstant.VmOperation operation;

    public List<String> getNicUuids() { return vNicUuids; }
    public void setNicUuids(List<String> uuids) { this.vNicUuids = uuids; }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public boolean isDeleteAllRules() {
        return deleteAllRules;
    }

    public void setDeleteAllRules(boolean deleteAllRules) {
        this.deleteAllRules = deleteAllRules;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public List<String> getSgUuids() {
        return sgUuids;
    }

    public void setSgUuids(List<String> sgUuids) {
        this.sgUuids = sgUuids;
    }

    public VmInstanceConstant.VmOperation getOperation() {
        return operation;
    }

    public void setOperation(VmInstanceConstant.VmOperation operation) {
        this.operation = operation;
    }

    public List<String> getvNicUuids() {
        return vNicUuids;
    }

    public void setvNicUuids(List<String> vNicUuids) {
        this.vNicUuids = vNicUuids;
    }
}
