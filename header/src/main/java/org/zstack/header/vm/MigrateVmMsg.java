package org.zstack.header.vm;

import org.zstack.header.allocator.AllocationScene;
import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

/**
 */
@SkipVmTracer(replyClass = MigrateVmReply.class)
public class MigrateVmMsg extends NeedReplyMessage implements VmInstanceMessage, MigrateVmMessage {
    private String vmInstanceUuid;
    private String strategy;
    private List<String> avoidHostUuids;
    private AllocationScene allocationScene;

    @Override
    public AllocationScene getAllocationScene() {
        return allocationScene;
    }

    public void setAllocationScene(AllocationScene allocationScene) {
        this.allocationScene = allocationScene;
    }

    public List<String> getAvoidHostUuids() {
        return avoidHostUuids;
    }

    public void setAvoidHostUuids(List<String> avoidHostUuids) {
        this.avoidHostUuids = avoidHostUuids;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    @Override
    public String getHostUuid() {
        return null;
    }

    @Override
    public boolean isMigrateFromDestination() {
        return false;
    }

    @Override
    public boolean isAllowUnknown() {
        return false;
    }

    @Override
    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }
}
