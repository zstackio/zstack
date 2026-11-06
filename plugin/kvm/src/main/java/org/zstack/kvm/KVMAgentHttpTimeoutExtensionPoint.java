package org.zstack.kvm;

import java.util.Set;

public interface KVMAgentHttpTimeoutExtensionPoint {
    Set<String> agentHttpPathsWithShortTimeout();
}
