package org.zstack.header.allocator;

/**
 */
public enum HostAllocatorError {
    NO_AVAILABLE_HOST(1000),
    NO_AVAILABLE_NIC(1001);

    private String code;

    private HostAllocatorError(int id) {
        code = String.format("HOST_ALLOCATION.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }
}
