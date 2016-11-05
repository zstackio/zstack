package org.zstack.header.vm;

/**
 */
public enum VmErrors {
    CREATE_ERROR(1000),
    START_ERROR(1001),
    STOP_ERROR(1002),
    REBOOT_ERROR(1003),
    DESTROY_ERROR(1004),
    ATTACH_VOLUME_ERROR(1005),
    DETACH_VOLUME_ERROR(1006),
    ATTACH_NETWORK_ERROR(1007),
    MIGRATE_ERROR(1008),
    ALLOCATE_MAC_ERROR(1009),
    NOT_IN_CORRECT_STATE(1010),
    CHANGE_VM_PASSWORD_ERROR(1011),
    SUSPEND_ERROR(1012),
    RESUME_ERROR(1013);


    private String code;

    private VmErrors(int id) {
        code = String.format("VM.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }
}
