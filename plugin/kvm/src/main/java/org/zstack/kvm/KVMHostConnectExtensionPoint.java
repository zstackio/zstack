package org.zstack.kvm;

public interface KVMHostConnectExtensionPoint {
    void kvmHostConnected(KVMHostConnectedContext context) throws KVMHostConnectException;
}
