package org.zstack.header.physicalserver;

public class ResourceConsumerHandle {
    public static final String SYSTEMD_UNIT = "SYSTEMD_UNIT";

    private String handleType;
    private String value;
    private String serviceName;
    private boolean optional;
    private boolean restartable;

    public ResourceConsumerHandle() {
    }

    public String getHandleType() {
        return handleType;
    }

    public void setHandleType(String handleType) {
        this.handleType = handleType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public boolean isRestartable() {
        return restartable;
    }

    public void setRestartable(boolean restartable) {
        this.restartable = restartable;
    }

}
