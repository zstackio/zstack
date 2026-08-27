package org.zstack.header.physicalserver;

public class ResourceConsumerHandle {
    public static final String SYSTEMD_UNIT = "SYSTEMD_UNIT";
    public static final String OWNER_PID_FILE = "OWNER_PID_FILE";

    private String handleType;
    private String value;
    private String serviceName;
    private String consumerKey;
    private boolean optional;
    private boolean restartable;
    private String expectedCommandToken;

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

    public String getConsumerKey() {
        return consumerKey;
    }

    public void setConsumerKey(String consumerKey) {
        this.consumerKey = consumerKey;
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

    public String getExpectedCommandToken() {
        return expectedCommandToken;
    }

    public void setExpectedCommandToken(String expectedCommandToken) {
        this.expectedCommandToken = expectedCommandToken;
    }
}
