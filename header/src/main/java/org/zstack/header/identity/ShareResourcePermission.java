package org.zstack.header.identity;

public enum ShareResourcePermission {
    READ(SharedResourceVO.PERMISSION_READ),
    WRITE(SharedResourceVO.PERMISSION_WRITE);

    public final int code;
    ShareResourcePermission(int code) {
        this.code = code;
    }
}
