package org.zstack.header.server;

public enum ProvisionPhase {
    NotStarted,
    NetworkPrepared,
    PxeTriggered,
    Pinging,
    Done
}
