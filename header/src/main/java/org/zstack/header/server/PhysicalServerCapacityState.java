package org.zstack.header.server;

public enum PhysicalServerCapacityState {
    Initialized,
    Ready,
    Allocated,
    Recalculating,
    Stale
}
