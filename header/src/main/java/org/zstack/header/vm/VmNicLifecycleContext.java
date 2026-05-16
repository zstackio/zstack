package org.zstack.header.vm;

import org.zstack.header.vm.VmInstanceConstant.VmOperation;

import java.util.Objects;

public class VmNicLifecycleContext {
    private VmOperation operation;
    private String vmUuid;
    private String srcHostUuid;
    private String destHostUuid;
    private String lastHostUuid;

    public VmOperation getOperation() {
        return operation;
    }

    public void setOperation(VmOperation operation) {
        this.operation = operation;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    public String getSrcHostUuid() {
        return srcHostUuid;
    }

    public void setSrcHostUuid(String srcHostUuid) {
        this.srcHostUuid = srcHostUuid;
    }

    public String getDestHostUuid() {
        return destHostUuid;
    }

    public void setDestHostUuid(String destHostUuid) {
        this.destHostUuid = destHostUuid;
    }

    public String getLastHostUuid() {
        return lastHostUuid;
    }

    public void setLastHostUuid(String lastHostUuid) {
        this.lastHostUuid = lastHostUuid;
    }

    public boolean isStartWithChangedHost() {
        return operation == VmOperation.Start
                && lastHostUuid != null
                && destHostUuid != null
                && !Objects.equals(lastHostUuid, destHostUuid);
    }
}
