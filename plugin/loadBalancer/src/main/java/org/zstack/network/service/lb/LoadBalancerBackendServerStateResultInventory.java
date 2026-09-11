package org.zstack.network.service.lb;

public class LoadBalancerBackendServerStateResultInventory {
    private String backendType;
    private String vmNicUuid;
    private String serverIp;
    private String targetState;
    private String effectiveState;

    public String getBackendType() {
        return backendType;
    }

    public void setBackendType(String backendType) {
        this.backendType = backendType;
    }

    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
    }

    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    public String getTargetState() {
        return targetState;
    }

    public void setTargetState(String targetState) {
        this.targetState = targetState;
    }

    public String getEffectiveState() {
        return effectiveState;
    }

    public void setEffectiveState(String effectiveState) {
        this.effectiveState = effectiveState;
    }
}
