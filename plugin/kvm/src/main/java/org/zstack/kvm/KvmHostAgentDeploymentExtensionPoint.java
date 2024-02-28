package org.zstack.kvm;

import org.zstack.header.host.HostInventory;

public interface KvmHostGetExtraPackagesExtensionPoint {
    String getExtraPackages(HostInventory host);
}
