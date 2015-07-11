package org.zstack.header.identity;

/**
 */
public enum IdentityErrors {
    AUTHENTICATION_ERROR(1000),
    INVALID_SESSION(1001),
    PERMISSION_DENIED(1002);

    private String code;

    private IdentityErrors(int id) {
        code = String.format("ID.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }
}
