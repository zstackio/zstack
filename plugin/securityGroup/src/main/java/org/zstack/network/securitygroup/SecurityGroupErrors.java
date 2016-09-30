package org.zstack.network.securitygroup;

/**
 */
public enum  SecurityGroupErrors {
    ADD_NIC_ERROR(1000);

    private String code;

    private SecurityGroupErrors(int id) {
        code = String.format("SG.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }
}
