package org.zstack.header.network.sdncontroller;

public enum SdnControllerStatus {
    Connecting,
    Connected,
    Disconnected,
    /** ZNS-specific: wizard-init-sync is in progress */
    Syncing,
    /** ZNS-specific: wizard-init-sync completed successfully */
    Ready
}
