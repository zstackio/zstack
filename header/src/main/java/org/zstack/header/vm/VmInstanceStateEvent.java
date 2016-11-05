package org.zstack.header.vm;

public enum VmInstanceStateEvent {
    /* Below event are caused by user operation */
    starting,
    stopping,
    migrating,
    migrated,
    destroying,
    rebooting,
    destroyed,
    expunging,
    suspending,
    resuming,
    
    /* Below events are from zstack internal logic */
    unknown,
    
    /* Below event are from result of user operation or vm state reported by full sync */
    running,
    stopped,
    suspended,
}
