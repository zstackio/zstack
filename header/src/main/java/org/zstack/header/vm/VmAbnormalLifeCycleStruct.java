package org.zstack.header.vm;

/**
 * Created by frank on 11/1/2015.
 */
public class VmAbnormalLifeCycleStruct {
    public enum VmAbnormalLifeCycleOperation {
        VmStoppedOnTheSameHost,
        VmRunningFromIntermediateState,
        VmStoppedFromIntermediateState,
        VmPausedFromUnknownStateHostNotChanged,
        VmRunningFromUnknownStateHostNotChanged,
        VmRunningFromUnknownStateHostChanged,
        VmStoppedFromUnknownStateHostNotChanged,
        VmMigrateToAnotherHost,
        VmRunningOnTheHost
    }

    private VmAbnormalLifeCycleOperation operation;
    private VmInstanceInventory vmInstance;
    private String originalHostUuid;
    private String currentHostUuid;
    private VmInstanceState originalState;
    private VmInstanceState currentState;

    public VmAbnormalLifeCycleOperation getOperation() {
        return operation;
    }

    public void setOperation(VmAbnormalLifeCycleOperation operation) {
        this.operation = operation;
    }

    public VmInstanceInventory getVmInstance() {
        return vmInstance;
    }

    public void setVmInstance(VmInstanceInventory vmInstance) {
        this.vmInstance = vmInstance;
    }

    public String getOriginalHostUuid() {
        return originalHostUuid;
    }

    public void setOriginalHostUuid(String originalHostUuid) {
        this.originalHostUuid = originalHostUuid;
    }

    public String getCurrentHostUuid() {
        return currentHostUuid;
    }

    public void setCurrentHostUuid(String currentHostUuid) {
        this.currentHostUuid = currentHostUuid;
    }

    public VmInstanceState getOriginalState() {
        return originalState;
    }

    public void setOriginalState(VmInstanceState originalState) {
        this.originalState = originalState;
    }

    public VmInstanceState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(VmInstanceState currentState) {
        this.currentState = currentState;
    }
}
