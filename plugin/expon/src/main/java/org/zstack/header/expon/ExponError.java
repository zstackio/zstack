package org.zstack.header.expon;

public enum ExponError {
    INVALID_SESSION("02.0c.02.0001"),
    SESSION_EXPIRED("02.0c.02.0005"),
    VHOST_ALREADY_UNBIND_USS("02.e0.03.0010");

    private String code;

    ExponError(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return code;
    }
}
